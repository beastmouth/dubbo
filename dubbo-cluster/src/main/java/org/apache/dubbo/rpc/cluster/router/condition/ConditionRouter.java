/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.router.condition;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.ADDRESS_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.FORCE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.PRIORITY_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RUNTIME_KEY;

/**
 * ConditionRouter
 * It supports the conditional routing configured by "override://", in 2.6.x,
 * refer to https://dubbo.apache.org/en/docs/v2.7/user/examples/routing-rule/ .
 * For 2.7.x and later, please refer to {@link org.apache.dubbo.rpc.cluster.router.condition.config.ServiceRouter}
 * and {@link org.apache.dubbo.rpc.cluster.router.condition.config.AppRouter}
 * refer to https://dubbo.apache.org/zh/docs/v2.7/user/examples/routing-rule/ .
 */
public class ConditionRouter extends AbstractRouter {
    public static final String NAME = "condition";

    private static final Logger logger = LoggerFactory.getLogger(ConditionRouter.class);
    protected static final Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
    protected static Pattern ARGUMENTS_PATTERN = Pattern.compile("arguments\\[([0-9]+)\\]");
    protected Map<String, MatchPair> whenCondition;
    protected Map<String, MatchPair> thenCondition;

    private boolean enabled;

    public ConditionRouter(String rule, boolean force, boolean enabled) {
        this.force = force;
        this.enabled = enabled;
        if (enabled) {
            this.init(rule);
        }
    }

    public ConditionRouter(URL url) {
        this.url = url;
        this.priority = url.getParameter(PRIORITY_KEY, 0);
        this.force = url.getParameter(FORCE_KEY, false);
        this.enabled = url.getParameter(ENABLED_KEY, true);
        if (enabled) {
            init(url.getParameterAndDecoded(RULE_KEY));
        }
    }

    public void init(String rule) {
        try {
            if (rule == null || rule.trim().length() == 0) {
                throw new IllegalArgumentException("Illegal route rule!");
            }
            rule = rule.replace("consumer.", "").replace("provider.", "");
            int i = rule.indexOf("=>");
            // 服务消费者匹配规则
            String whenRule = i < 0 ? null : rule.substring(0, i).trim();
            // 服务提供者匹配规则
            String thenRule = i < 0 ? rule.trim() : rule.substring(i + 2).trim();
            // 解析规则
            Map<String, MatchPair> when = StringUtils.isBlank(whenRule) || "true".equals(whenRule) ? new HashMap<String, MatchPair>() : parseRule(whenRule);
            Map<String, MatchPair> then = StringUtils.isBlank(thenRule) || "false".equals(thenRule) ? null : parseRule(thenRule);
            // NOTE: It should be determined on the business level whether the `When condition` can be empty or not.
            // 将解析出来的规则赋值给成员变量
            this.whenCondition = when;
            this.thenCondition = then;
        } catch (ParseException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static Map<String, MatchPair> parseRule(String rule)
            throws ParseException {
        // 定义条件映射集合
        Map<String, MatchPair> condition = new HashMap<String, MatchPair>();
        if (StringUtils.isBlank(rule)) {
            return condition;
        }
        // Key-Value pair, stores both match and mismatch conditions
        MatchPair pair = null;
        // Multiple values
        Set<String> values = null;
        // 通过正则表达式匹配路由规则，ROUTE_PATTERN = ([&!=,]*)\s*([^&!=,\s]+)
        // 这个表达式看起来不是很好理解，第一个括号内的表达式用于匹配"&", "!", "=" 和 "," 等符号。
        // 第二括号内的用于匹配英文字母，数字等字符。举个例子说明一下：
        //    host = 2.2.2.2 & host != 1.1.1.1 & method = hello
        // 匹配结果如下：
        //     括号一      括号二
        // 1.  null       host
        // 2.   =         2.2.2.2
        // 3.   &         host
        // 4.   !=        1.1.1.1
        // 5.   &         method
        // 6.   =         hello
        // 以上面的例子为例，匹配完成后的效果为
        // {
        //    "host": {
        //        "matches": ["2.2.2.2"],
        //        "mismatches": ["1.1.1.1"]
        //    },
        //    "method": {
        //        "matches": ["hello"],
        //        "mismatches": []
        //    }
        // }
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        while (matcher.find()) { // Try to match one by one
            // 获取括号一内的匹配结果
            String separator = matcher.group(1);
            // 获取括号二内的匹配结果
            String content = matcher.group(2);
            // Start part of the condition expression.
            // 分隔符为空，表示匹配的是表达式的开始部分
            if (StringUtils.isEmpty(separator)) {
                // 创建 MatchPair 对象
                pair = new MatchPair();
                // 存储 <匹配项, MatchPair> 键值对，比如 <host, MatchPair>
                condition.put(content, pair);
            }
            // The KV part of the condition expression
            else if ("&".equals(separator)) {
                if (condition.get(content) == null) {
                    pair = new MatchPair();
                    condition.put(content, pair);
                } else {
                    pair = condition.get(content);
                }
            }
            // The Value in the KV part.
            else if ("=".equals(separator)) {
                if (pair == null) {
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                }

                values = pair.matches;
                values.add(content);
            }
            // The Value in the KV part.
            else if ("!=".equals(separator)) {
                if (pair == null) {
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                }

                values = pair.mismatches;
                values.add(content);
            }
            // The Value in the KV part, if Value have more than one items.
            else if (",".equals(separator)) { // Should be separated by ','
                if (values == null || values.isEmpty()) {
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                }
                values.add(content);
            } else {
                throw new ParseException("Illegal route rule \"" + rule
                        + "\", The error char '" + separator + "' at index "
                        + matcher.start() + " before \"" + content + "\".", matcher.start());
            }
        }
        return condition;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation)
            throws RpcException {
        if (!enabled) {
            return invokers;
        }

        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }
        try {
            // 先对服务消费者条件进行匹配，如果匹配失败，表明服务消费者 url 不符合匹配规则，
            // 无需进行后续匹配，直接返回 Invoker 列表即可。比如下面的规则：
            //     host = 10.20.153.10 => host = 10.0.0.10
            // 这条路由规则希望 IP 为 10.20.153.10 的服务消费者调用 IP 为 10.0.0.10 机器上的服务。
            // 当消费者 ip 为 10.20.153.11 时，matchWhen 返回 false，表明当前这条路由规则不适用于
            // 当前的服务消费者，此时无需再进行后续匹配，直接返回即可。
            if (!matchWhen(url, invocation)) {
                return invokers;
            }
            List<Invoker<T>> result = new ArrayList<Invoker<T>>();
            // 服务提供者匹配条件未配置，表明对指定的服务消费者禁用服务，也就是服务消费者在黑名单中
            if (thenCondition == null) {
                logger.warn("The current consumer in the service blacklist. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey());
                return result;
            }
            // 这里可以简单的把 Invoker 理解为服务提供者，现在使用服务提供者匹配规则对
            // Invoker 列表进行匹配
            for (Invoker<T> invoker : invokers) {
                // 若匹配成功，表明当前 Invoker 符合服务提供者匹配规则。
                // 此时将 Invoker 添加到 result 列表中
                if (matchThen(invoker.getUrl(), url)) {
                    result.add(invoker);
                }
            }
            // 返回匹配结果，如果 result 为空列表，且 force = true，表示强制返回空列表，
            // 否则路由结果为空的路由规则将自动失效
            if (!result.isEmpty()) {
                return result;
            } else if (force) {
                logger.warn("The route result is empty and force execute. consumer: " + NetUtils.getLocalHost() + ", service: " + url.getServiceKey() + ", router: " + url.getParameterAndDecoded(RULE_KEY));
                return result;
            }
        } catch (Throwable t) {
            logger.error("Failed to execute condition router rule: " + getUrl() + ", invokers: " + invokers + ", cause: " + t.getMessage(), t);
        }
        return invokers;
    }

    @Override
    public boolean isRuntime() {
        // We always return true for previously defined Router, that is, old Router doesn't support cache anymore.
//        return true;
        return this.url.getParameter(RUNTIME_KEY, false);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    boolean matchWhen(URL url, Invocation invocation) {
        // 服务消费者条件为 null 或空，均返回 true，比如：
        //     => host != 172.22.3.91
        // 表示所有的服务消费者都不得调用 IP 为 172.22.3.91 的机器上的服务
        return CollectionUtils.isEmptyMap(whenCondition) || matchCondition(whenCondition /*服务消费者匹配条件*/, url /*源自route方法的参数列表，由外部调用类调用route时传入*/, null, invocation);
    }

    private boolean matchThen(URL url, URL param) {
        return CollectionUtils.isNotEmptyMap(thenCondition) && matchCondition(thenCondition, url, param, null);
    }

    private boolean matchCondition(Map<String, MatchPair> condition, URL url, URL param, Invocation invocation) {
        // 将服务提供者或消费者 url 转成 Map
        Map<String, String> sample = url.toMap();
        boolean result = false;
        // 遍历 condition 列表
        for (Map.Entry<String, MatchPair> matchPair : condition.entrySet()) {
            // 获取匹配项名称，比如 host、method 等
            String key = matchPair.getKey();

            if (key.startsWith(Constants.ARGUMENTS)) {
                if (!matchArguments(matchPair, invocation)) {
                    return false;
                } else {
                    result = true;
                    continue;
                }
            }

            String sampleValue;
            //get real invoked method name from invocation
            // 如果 invocation 不为空，且 key 为 method(s)，表示进行方法匹配
            if (invocation != null && (METHOD_KEY.equals(key) || METHODS_KEY.equals(key))) {
                // 从 invocation 获取被调用方法的名称
                sampleValue = invocation.getMethodName();
            } else if (ADDRESS_KEY.equals(key)) {
                sampleValue = url.getAddress();
            } else if (HOST_KEY.equals(key)) {
                sampleValue = url.getHost();
            } else {
                // 从服务提供者或消费者 url 中获取指定字段值，比如 host、application 等
                sampleValue = sample.get(key);
                if (sampleValue == null) {
                    // 尝试通过 default.xxx 获取相应的值
                    sampleValue = sample.get(key);
                }
            }
            if (sampleValue != null) {
                // 调用 MatchPair 的 isMatch 方法进行匹配
                if (!matchPair.getValue().isMatch(sampleValue, param)) {
                    // 只要有一个规则匹配失败，立即返回 false 结束方法逻辑
                    return false;
                } else {
                    result = true;
                }
            } else {
                //not pass the condition
                // sampleValue 为空，表明服务提供者或消费者 url 中不包含相关字段。此时如果
                // MatchPair 的 matches 不为空，表示匹配失败，返回 false。比如我们有这样
                // 一条匹配条件 loadbalance = random，假设 url 中并不包含 loadbalance 参数，
                // 此时 sampleValue = null。既然路由规则里限制了 loadbalance 必须为 random，
                // 但 sampleValue = null，明显不符合规则，因此返回 false
                if (!matchPair.getValue().matches.isEmpty()) {
                    return false;
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * analysis the arguments in the rule.
     * Examples would be like this:
     * "arguments[0]=1", whenCondition is that the first argument is equal to '1'.
     * "arguments[1]=a", whenCondition is that the second argument is equal to 'a'.
     * @param matchPair
     * @param invocation
     * @return
     */
    public boolean matchArguments(Map.Entry<String, MatchPair> matchPair, Invocation invocation) {
        try {
            // split the rule
            String key = matchPair.getKey();
            String[] expressArray = key.split("\\.");
            String argumentExpress = expressArray[0];
            final Matcher matcher = ARGUMENTS_PATTERN.matcher(argumentExpress);
            if (!matcher.find()) {
                return false;
            }

            //extract the argument index
            int index = Integer.parseInt(matcher.group(1));
            if (index < 0 || index > invocation.getArguments().length) {
                return false;
            }

            //extract the argument value
            Object object = invocation.getArguments()[index];

            if (matchPair.getValue().isMatch(String.valueOf(object), null)) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("Arguments match failed, matchPair[]" + matchPair + "] invocation[" + invocation + "]", e);
        }

        return false;
    }

    protected static final class MatchPair {
        final Set<String> matches = new HashSet<String>();
        final Set<String> mismatches = new HashSet<String>();

        private boolean isMatch(String value, URL param) {
            if (!matches.isEmpty() && mismatches.isEmpty()) {
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                return false;
            }

            if (!mismatches.isEmpty() && matches.isEmpty()) {
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                return true;
            }

            if (!matches.isEmpty() && !mismatches.isEmpty()) {
                //when both mismatches and matches contain the same value, then using mismatches first
                for (String mismatch : mismatches) {
                    if (UrlUtils.isMatchGlobPattern(mismatch, value, param)) {
                        return false;
                    }
                }
                for (String match : matches) {
                    if (UrlUtils.isMatchGlobPattern(match, value, param)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }
    }
}

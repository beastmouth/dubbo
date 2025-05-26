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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

import static org.apache.dubbo.rpc.Constants.PROXY_KEY;

/**
 * ProxyFactory. (API/SPI, Singleton, ThreadSafe)
 */
@SPI("javassist")
public interface ProxyFactory {

    /**
     * create proxy.
     *
     * @param invoker
     * @return proxy
     */
    @Adaptive({PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;

    /**
     * create proxy.
     *
     * @param invoker
     * @return proxy
     */
    @Adaptive({PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException;

    /**
     * create invoker.
     *
     * @param <T>
     * @param proxy proxy 命名不太符合实际语义，应该改为 target 更合适，因为传入的实际上是 rpc 服务实现类实例，如 DemoService 的实现 DemoServiceImpl
     * @param type rpc 服务接口，如 DemoService
     * @param url 注册中心 url，且 export 参数为发布到注册中心的 url 如
     * registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=heihei-app&dubbo=2.0.2&pid=3337&registry=zookeeper&timestamp=1678540659987
     * &
     * export=dubbo://127.0.0.1:20881/org.apache.dubbo.demo.DemoService?
     * anyhost=true&application=heihei-app&bind.ip=127.0.0.1&bind.port=20881&
     * deprecated=false&dubbo=2.0.2&
     * dynamic=true&generic=false&
     * interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&
     * pid=3337&release=&scope=remote&side=provider&timestamp=1678540660025
     * @return invoker
     */
    @Adaptive({PROXY_KEY})
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException;

}
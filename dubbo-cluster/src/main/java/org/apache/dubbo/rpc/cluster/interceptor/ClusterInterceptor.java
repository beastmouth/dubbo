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
package org.apache.dubbo.rpc.cluster.interceptor;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

/**
 * Different from {@link Filter}, ClusterInterceptor works at the outmost layer, before one specific address/invoker is picked.
 */
@SPI
public interface ClusterInterceptor {

    // 发起rpc请求前，创建rpc请求上下文，清除rpc响应上下文
    void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation);

    // // 发起rpc请求完成，还未收到rpc响应，清除rpc请求上下文
    void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation);

    /**
     * Does not need to override this method, override {@link #before(AbstractClusterInvoker, Invocation)}
     * and {@link #after(AbstractClusterInvoker, Invocation)}, methods to add your own logic expected to be
     * executed before and after invoke.
     *
     * @param clusterInvoker
     * @param invocation
     * @return
     * @throws RpcException
     */
    default Result intercept(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) throws RpcException {
        return clusterInvoker.invoke(invocation);
    }

    interface Listener {

        // 收到rpc响应，且响应成功，创建rpc响应上下文
        void onMessage(Result appResponse, AbstractClusterInvoker<?> clusterInvoker, Invocation invocation);

        // rpc响应失败，什么都不做
        void onError(Throwable t, AbstractClusterInvoker<?> clusterInvoker, Invocation invocation);
    }
}

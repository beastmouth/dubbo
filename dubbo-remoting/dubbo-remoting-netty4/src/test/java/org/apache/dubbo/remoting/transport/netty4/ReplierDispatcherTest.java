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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.ReplierDispatcher;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ReplierDispatcherTest
 */
class ReplierDispatcherTest {

    private ExchangeServer exchangeServer;

    private ConcurrentHashMap<String, ExchangeChannel> clients = new ConcurrentHashMap<>();

    private int port;

    @BeforeEach
    public void startServer() throws RemotingException {
        FrameworkModel.destroyAll();
        port = NetUtils.getAvailablePort();
        ReplierDispatcher dispatcher = new ReplierDispatcher();
        dispatcher.addReplier(RpcMessage.class, new RpcMessageHandler());
        dispatcher.addReplier(Data.class, (channel, msg) -> new StringMessage("hello world"));
        URL url = URL.valueOf(
                "exchange://localhost:" + port + "?" + CommonConstants.TIMEOUT_KEY + "=60000&threadpool=cached");
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");
        applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_DEFAULT);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ConfigManager configManager = new ConfigManager(applicationModel);
        configManager.setApplication(applicationConfig);
        configManager.getApplication();
        applicationModel.setConfigManager(configManager);
        url = url.setScopeModel(applicationModel);
        ModuleModel moduleModel = applicationModel.getDefaultModule();
        url = url.putAttribute(CommonConstants.SCOPE_MODEL, moduleModel);
        exchangeServer = Exchangers.bind(url, dispatcher);
    }

    @Test
    void testDataPackage() throws Exception {
        ExchangeChannel client = Exchangers.connect(
                URL.valueOf("exchange://localhost:" + port + "?" + CommonConstants.TIMEOUT_KEY + "=60000"));
        Random random = new Random();
        for (int i = 5; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i * 100; j++)
                sb.append('(').append(random.nextLong()).append(')');
            Data d = new Data();
            d.setData(sb.toString());
            Assertions.assertEquals(client.request(d).get().toString(), "hello world");
        }
        clients.put(Thread.currentThread().getName(), client);
    }

    @Test
    void testMultiThread() throws Exception {
        int tc = 10;
        ExecutorService exec = Executors.newFixedThreadPool(tc);
        List<Future<?>> futureList = new LinkedList<>();
        for (int i = 0; i < tc; i++)
            futureList.add(exec.submit(() -> {
                try {
                    clientExchangeInfo(port);
                } catch (Exception e) {
                    fail(e);
                }
            }));
        for (Future<?> future : futureList) {
            future.get();
        }
        exec.shutdown();
        exec.awaitTermination(10, TimeUnit.SECONDS);
    }

    void clientExchangeInfo(int port) throws Exception {
        ExchangeChannel client = Exchangers.connect(
                URL.valueOf("exchange://localhost:" + port + "?" + CommonConstants.TIMEOUT_KEY + "=60000"));
        clients.put(Thread.currentThread().getName(), client);
        MockResult result = (MockResult) client.request(new RpcMessage(
                        DemoService.class.getName(), "plus", new Class<?>[] {int.class, int.class}, new Object[] {55, 25
                        }))
                .get();
        Assertions.assertEquals(result.getResult(), 80);
        for (int i = 0; i < 100; i++) {
            client.request(new RpcMessage(
                    DemoService.class.getName(), "sayHello", new Class<?>[] {String.class}, new Object[] {"qianlei" + i
                    }));
        }
        for (int i = 0; i < 100; i++) {
            CompletableFuture<Object> future = client.request(new Data());
            Assertions.assertEquals(future.get().toString(), "hello world");
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            if (exchangeServer != null) exchangeServer.close();
        } finally {
            if (clients.size() != 0)
                clients.forEach((key, value) -> {
                    value.close();
                    clients.remove(key);
                });
        }
    }

    static class Data implements Serializable {
        private static final long serialVersionUID = -4666580993978548778L;

        private String mData = "";

        public Data() {}

        public String getData() {
            return mData;
        }

        public void setData(String data) {
            mData = data;
        }
    }

    static class StringMessage implements Serializable {
        private static final long serialVersionUID = 7193122183120113947L;

        private String mText;

        StringMessage(String msg) {
            mText = msg;
        }

        public String toString() {
            return mText;
        }
    }
}

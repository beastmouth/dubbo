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
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.utils.ReferenceConfigCache;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.GreetingService;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Application {
    public static void main(String[] args) throws Exception {
        if (isClassic(args)) {
            startWithExport();
        } else {
            startWithBootstrap();
        }
    }

    private static boolean isClassic(String[] args) {
        return args.length > 0 && "classic".equalsIgnoreCase(args[0]);
    }

    private static void startWithBootstrap() {
        // 创建一个ServiceConfig的实例，泛型参数是业务接口实现类 即DemoServiceImpl
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        // 指定业务接口
        service.setInterface(DemoService.class);
        // 指定业务接口的实现，由该对象来处理Consumer的请求
        service.setRef(new DemoServiceImpl());
        service.setId(UUID.randomUUID().toString());

        // 注册多个api的话，需要制定下service id，否则会被认为重复（TODO 具体原因equal认为重复未看）
        ServiceConfig<GreetingServiceImpl> service2 = new ServiceConfig<>();
        service2.setInterface(GreetingService.class);
        service2.setRef(new GreetingServiceImpl());
        service2.setId(UUID.randomUUID().toString());

        // 模拟injvm
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        reference.setInterface(DemoService.class);
        reference.setGeneric("false");

        Thread testInJvmThread = new Thread(new TestInJvm(reference));
        testInJvmThread.start();

        // 获取DubboBootstrap实例，这是个单例的对象
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        // 生成一个 ApplicationConfig 的实例、指定ZK地址以及ServiceConfig实例
        bootstrap.application(new ApplicationConfig("dubbo-demo-api-provider"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .service(service)
                .service(service2)
                .reference(reference)
                .start()
                .await();
    }

    private static void startWithExport() throws InterruptedException {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setApplication(new ApplicationConfig("dubbo-demo-api-provider"));
        service.setRegistry(new RegistryConfig("zookeeper://127.0.0.1:2181"));
        service.export();

        System.out.println("dubbo service started");
        new CountDownLatch(1).await();
    }
}

class TestInJvm implements Runnable {
    private final ReferenceConfig<DemoService> reference;

    public TestInJvm(ReferenceConfig<DemoService> reference) {
        this.reference = reference;
    }

    @Override
    public void run() {
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DemoService demoService = ReferenceConfigCache.getCache().get(reference);
        String message = demoService.sayHello("dubbo");
        System.out.println(message);
    }
}

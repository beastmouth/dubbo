package org.apache.dubbo.demo.consumer.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;

/**
 * @author hbj
 * @since 2021/6/1 4:42 下午
 */
public class TestDemo {
    public static void main(String[] args) {
        ExtensionLoader<LoginGranter> extensionLoader = ExtensionLoader.getExtensionLoader(LoginGranter.class);
        LoginGranter loginGranter = extensionLoader.getAdaptiveExtension();
        System.out.println(loginGranter.getClass().getName());

        URL url1 = URL.valueOf("dubbo://127.0.0.1:20880");
        loginGranter.login(url1);
        System.out.println(loginGranter);

        URL url2 = URL.valueOf("dubbo://127.0.0.1:20880?method=wechat");
        loginGranter.login(url2);
        System.out.println(loginGranter);
    }
}

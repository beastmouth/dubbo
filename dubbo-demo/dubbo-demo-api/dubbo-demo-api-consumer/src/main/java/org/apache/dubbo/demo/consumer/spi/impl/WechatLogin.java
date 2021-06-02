package org.apache.dubbo.demo.consumer.spi.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.demo.consumer.spi.LoginGranter;

/**
 * @author hbj
 * @since 2021/6/1 4:39 下午
 */
public class WechatLogin implements LoginGranter {
    @Override
    public void login(URL url) {
        System.out.println("Wechat Login");
    }
}

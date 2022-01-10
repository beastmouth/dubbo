package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.TestVoidService;

/**
 * @author hbj
 * @since 2022/1/10 5:50 下午
 */
public class TestVoidServiceImpl implements TestVoidService {
    @Override
    public void hello() {
        System.out.println("hello world");
    }
}

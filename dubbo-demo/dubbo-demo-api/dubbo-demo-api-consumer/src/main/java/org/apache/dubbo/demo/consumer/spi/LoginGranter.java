package org.apache.dubbo.demo.consumer.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

/**
 * @author hbj
 * @since 2021/6/1 4:36 下午
 */
@SPI("qq")
public interface LoginGranter {
    @Adaptive("method")
    void login(URL url);
}

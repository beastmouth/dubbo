package org.apache.dubbo.demo.consumer.spitest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.demo.consumer.spitest.entity.Wheel;

@SPI
public interface WheelMaker {
    @Adaptive("wheel")
    Wheel makeWheel(URL url);
}

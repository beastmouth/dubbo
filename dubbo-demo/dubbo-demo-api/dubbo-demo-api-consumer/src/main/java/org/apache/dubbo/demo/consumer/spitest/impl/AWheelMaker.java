package org.apache.dubbo.demo.consumer.spitest.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.demo.consumer.spitest.WheelMaker;
import org.apache.dubbo.demo.consumer.spitest.entity.Wheel;

/**
 * @author huangbangjing
 * @date 2022/11/16 16:37
 */
public class AWheelMaker implements WheelMaker {
    @Override
    public Wheel makeWheel(URL url) {
        Wheel wheel = new Wheel();
        wheel.setName("a wheel");
        return wheel;
    }
}

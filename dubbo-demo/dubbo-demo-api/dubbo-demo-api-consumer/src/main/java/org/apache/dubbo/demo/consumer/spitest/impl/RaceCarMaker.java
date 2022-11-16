package org.apache.dubbo.demo.consumer.spitest.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.demo.consumer.spitest.CarMaker;
import org.apache.dubbo.demo.consumer.spitest.WheelMaker;
import org.apache.dubbo.demo.consumer.spitest.entity.Car;
import org.apache.dubbo.demo.consumer.spitest.entity.Wheel;

/**
 * @author huangbangjing
 * @date 2022/11/16 16:33
 */
public class RaceCarMaker implements CarMaker {
    private WheelMaker wheelMaker;

    public void setWheelMaker(WheelMaker wheelMaker) {
        this.wheelMaker = wheelMaker;
    }

    @Override
    public Car makeCar(URL url) {
        Wheel wheel = wheelMaker.makeWheel(url);
        return new Car(wheel);
    }
}

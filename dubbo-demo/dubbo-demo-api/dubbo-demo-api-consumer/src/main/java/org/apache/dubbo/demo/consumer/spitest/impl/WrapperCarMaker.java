package org.apache.dubbo.demo.consumer.spitest.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.demo.consumer.spitest.CarMaker;
import org.apache.dubbo.demo.consumer.spitest.entity.Car;

/**
 * @author huangbangjing
 * @date 2022/11/16 22:23
 */
public class WrapperCarMaker implements CarMaker {
    private CarMaker carMaker;

    public WrapperCarMaker(CarMaker carMaker) {
        this.carMaker = carMaker;
    }

    @Override
    public Car makeCar(URL url) {
        System.out.println("before:包装类==do some thing");
        Car car = carMaker.makeCar(url);
        System.out.println("after:包装类==do some thing");
        return car;
    }
}

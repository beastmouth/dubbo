package org.apache.dubbo.demo.consumer.spitest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.demo.consumer.spitest.entity.Car;

@SPI
public interface CarMaker {
    Car makeCar(URL url);
}

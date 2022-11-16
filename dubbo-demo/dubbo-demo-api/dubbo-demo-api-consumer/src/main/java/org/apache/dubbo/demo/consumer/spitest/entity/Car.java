package org.apache.dubbo.demo.consumer.spitest.entity;

/**
 * @author huangbangjing
 * @date 2022/11/16 16:32
 */
public class Car {
    private Wheel wheel;

    public Car(Wheel wheel) {
        this.wheel = wheel;
    }

    public Car() {

    }

    public Wheel getWheel() {
        return wheel;
    }

    public void setWheel(Wheel wheel) {
        this.wheel = wheel;
    }

    @Override
    public String toString() {
        return "Car{" +
                "wheel=" + wheel +
                '}';
    }
}

package org.apache.dubbo.demo.consumer.spitest.entity;

/**
 * @author huangbangjing
 * @date 2022/11/16 16:32
 */
public class Wheel {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Wheel{" +
                "name='" + name + '\'' +
                '}';
    }
}

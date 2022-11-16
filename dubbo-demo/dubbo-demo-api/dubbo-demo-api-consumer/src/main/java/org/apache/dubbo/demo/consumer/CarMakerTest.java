package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.demo.consumer.spitest.CarMaker;
import org.apache.dubbo.demo.consumer.spitest.entity.Car;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author huangbangjing
 * @date 2022/11/16 17:03
 */
public class CarMakerTest {

    @Test
    public void test() {
        HashMap<String, String> param = new HashMap<>();
        param.put("wheel", "awheel");
        URL url = new URL("", "", -1, param);
//        CarMaker carMaker = ExtensionLoader.getExtensionLoader(CarMaker.class).getExtension("race");
        ExtensionLoader<CarMaker> extensionLoader = ExtensionLoader.getExtensionLoader(CarMaker.class);
        Car car = extensionLoader.getExtension("race").makeCar(url);
        System.out.println(car.toString());

        url = url.addParameter("wheel", "bwheel");
        car = extensionLoader.getExtension("race").makeCar(url);
        System.out.println(car.toString());
    }
}

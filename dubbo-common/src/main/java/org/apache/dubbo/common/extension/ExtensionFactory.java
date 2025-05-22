/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.extension;

/**
 * ExtensionFactory
 * 这里走硬编码实现的AdaptiveExtensionFactory，循环每个ExtensionFactory扩展点，通过type和name找扩展点实现。
 * ExtensionFactory有两个实现
 * 原生的SpiExtensionFactory，没有利用setter的属性name，直接获取type对应的自适应扩展点。
 * Spring相关的SpringExtensionFactory支持从多个ioc容器中，通过getBean(setter属性名,扩展点)获取bean。
 */
@SPI
public interface ExtensionFactory {

    /**
     * Get extension.
     *
     * @param type object type.
     * @param name object name.
     * @return object instance.
     */
    <T> T getExtension(Class<T> type, String name);

}

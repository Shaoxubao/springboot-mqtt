/**
 * Copyright © 2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baoge.gateway;

import com.alibaba.fastjson.JSONObject;
import com.baoge.service.MqttDeliveryFuture;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 16.01.17.
 */
public interface GatewayService {

    void init() throws Exception;

    void destroy() throws Exception;

    String getTenantLabel();

    /**
     * Inform gateway service that device is connected
     *
     * @param deviceName
     * @param deviceType
     */
    MqttDeliveryFuture onDeviceConnect(String deviceName, String deviceType);

    /**
     * Inform gateway service that device is disconnected
     *
     * @param deviceName
     */
    Optional<MqttDeliveryFuture> onDeviceDisconnect(String deviceName);

    MqttDeliveryFuture onDeviceTelemetry(JSONObject body);
}

/**
 * Copyright © 2017 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baoge.gateway;

import com.alibaba.fastjson.JSONObject;
import com.baoge.config.MqttProviderConfig;
import com.baoge.data.DeviceInfo;
import com.baoge.service.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.baoge.utils.JsonTools.newNode;
import static com.baoge.utils.JsonTools.toBytes;

/**
 *
 */
@Slf4j
public class MqttGatewayService implements GatewayService {
    private static final String GATEWAY_TELEMETRY_TOPIC = "topic";
    private static final String GATEWAY_CONNECT_TOPIC = "v1/gateway/connect";
    private static final String GATEWAY_DISCONNECT_TOPIC = "v1/gateway/disconnect";
    private static final long DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final long DEFAULT_POLLING_INTERVAL = 1000;

    private final ConcurrentMap<String, DeviceInfo> devices = new ConcurrentHashMap<>();
    private final AtomicLong attributesCount = new AtomicLong();
    private final AtomicLong telemetryCount = new AtomicLong();
    private final AtomicInteger msgIdSeq = new AtomicInteger();

    private String tenantLabel;

    private PersistentFileService persistentFileService;

    private MqttClient client;

    private ExecutorService mqttSenderExecutor;
    private ExecutorService mqttReceiverExecutor;
    private ExecutorService callbackExecutor = Executors.newCachedThreadPool();

    @Autowired
    private MqttProviderConfig mqttProviderConfig;

    public MqttGatewayService() {
    }

    @Override
    @PostConstruct
    public void init() {
        BlockingQueue<MessageFuturePair> incomingQueue = new LinkedBlockingQueue<>();
        initMqttClient();
        initMqttSender(incomingQueue);
//        initMqttReceiver(incomingQueue);
    }

    @Override
    public void destroy() throws Exception {
        callbackExecutor.shutdownNow();
        mqttSenderExecutor.shutdownNow();
        mqttReceiverExecutor.shutdownNow();
        client.disconnect();
    }

    @Override
    public String getTenantLabel() {
        return tenantLabel;
    }

    private void initMqttClient() {
        client = mqttProviderConfig.connect();
    }

    private void initMqttSender(BlockingQueue<MessageFuturePair> incomingQueue) {
        mqttSenderExecutor = Executors.newSingleThreadExecutor();
        mqttSenderExecutor.submit(new MqttMessageSender(client, persistentFileService, incomingQueue));
    }

    private void initMqttReceiver(BlockingQueue<MessageFuturePair> incomingQueue) {
        mqttReceiverExecutor = Executors.newSingleThreadExecutor();
        mqttReceiverExecutor.submit(new MqttMessageReceiver(persistentFileService, incomingQueue, 1000));
    }

    @Override
    public MqttDeliveryFuture onDeviceConnect(final String deviceName, final String deviceType) {
        final int msgId = msgIdSeq.incrementAndGet();
        ObjectNode gwMsg = newNode().put("device", deviceName);
        gwMsg.put("type", deviceType);
        byte[] msgData = toBytes(gwMsg);
        log.info("[{}] Device Connected!", deviceName);
        devices.putIfAbsent(deviceName, new DeviceInfo(deviceName, deviceType));
        return persistMessage(GATEWAY_CONNECT_TOPIC, msgId, msgData, deviceName,
                message -> {
                    log.info("[{}][{}][{}] Device connect event is reported to Thingsboard!", deviceName, deviceType, msgId);
                },
                error -> log.warn("[{}][{}] Failed to report device connection!", deviceName, msgId, error));

    }

    @Override
    public Optional<MqttDeliveryFuture> onDeviceDisconnect(String deviceName) {
        if (deviceName != null && devices.remove(deviceName) != null) {
            final int msgId = msgIdSeq.incrementAndGet();
            byte[] msgData = toBytes(newNode().put("device", deviceName));
            log.info("[{}][{}] Device Disconnected!", deviceName, msgId);
            return Optional.ofNullable(persistMessage(GATEWAY_DISCONNECT_TOPIC, msgId, msgData, deviceName,
                    message -> {
                        log.info("[{}][{}] Device disconnect event is delivered!", deviceName, msgId);
                    },
                    error -> log.warn("[{}][{}] Failed to report device disconnect!", deviceName, msgId, error)));
        } else {
            log.debug("[{}] Device was disconnected before. Nothing is going to happened.", deviceName);
            return Optional.empty();
        }
    }

    @Override
    public MqttDeliveryFuture onDeviceTelemetry(JSONObject body) {
        final int msgId = msgIdSeq.incrementAndGet();
        // System.out.println("body.Bytes.length "+ body.toJSONString().getBytes().length);
        return persistMessage(GATEWAY_TELEMETRY_TOPIC, msgId, body.toJSONString().getBytes(), null,
                message -> {
                    log.debug("[{}][{}] Device telemetry published to ThingsBoard!", msgId, null);
                    telemetryCount.addAndGet(0);
                },
                error -> log.warn("[{}][{}] Failed to publish device telemetry!", null, msgId, error));
    }


    private MqttDeliveryFuture persistMessage(String topic,
                                              int msgId,
                                              byte[] payload,
                                              String deviceId,
                                              Consumer<Void> onSuccess,
                                              Consumer<Throwable> onFailure) {
        try {
            return persistentFileService.persistMessage(topic, msgId, payload, deviceId, onSuccess, onFailure);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void setPersistentFileService(PersistentFileService persistentFileService) {
        this.persistentFileService = persistentFileService;
    }
}

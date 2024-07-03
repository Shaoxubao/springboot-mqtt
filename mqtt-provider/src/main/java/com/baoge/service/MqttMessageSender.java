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
package com.baoge.service;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by Valerii Sosliuk on 12/12/2017.
 */
@Slf4j
public class MqttMessageSender implements Runnable {

    private MqttClient tbClient;

    private PersistentFileService persistentFileService;

    private BlockingQueue<MessageFuturePair> incomingQueue;
    private Queue<Future<Void>> outgoingQueue;

    public MqttMessageSender(MqttClient tbClient,
                             PersistentFileService persistentFileService,
                             BlockingQueue<MessageFuturePair> incomingQueue) {
        this.tbClient = tbClient;
        this.persistentFileService = persistentFileService;
        this.incomingQueue = incomingQueue;
        outgoingQueue = new ConcurrentLinkedQueue();

    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                checkClientConnected();
                if (!checkOutgoingQueueIsEmpty()) {
                    log.debug("Waiting until all messages are sent before going to the next bucket");
                    Thread.sleep(1000);
                    continue;
                }
                List<MqttPersistentMessage> storedMessages = getMessages();
                if (!storedMessages.isEmpty()) {
                    Iterator<MqttPersistentMessage> iter = storedMessages.iterator();
                    while (iter.hasNext()) {
                        if (!checkClientConnected()) {
                            persistentFileService.saveForResend(Lists.newArrayList(iter));
                            break;
                        }
                        MqttPersistentMessage message = iter.next();
                        log.debug("Sending message [{}]", message);
                        publishMqttMessage(message);
                        log.info("publish mqtt message finished.");
//                        outgoingQueue.add(publishFuture);
                    }
                } else {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                log.trace(e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    private void publishMqttMessage(MqttPersistentMessage message) {
        try {
            tbClient.publish(message.getTopic(), message.getPayload(), MqttQoS.AT_LEAST_ONCE.value(), false);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkOutgoingQueueIsEmpty() {
        if (!outgoingQueue.isEmpty()) {
            int pendingCount = 0;
            boolean allFinished = true;
            for (Future<Void> future : outgoingQueue) {
                if (!future.isDone()) {
                    pendingCount++;
                }
                allFinished &= future.isDone();
            }
            if (allFinished) {
                outgoingQueue = new ConcurrentLinkedQueue();
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean checkClientConnected() {
        if (!tbClient.isConnected()) {
            try {
                clearOutgoingQueue();
                log.info("ThingsBoard MQTT connection failed. Reconnecting in [{}] milliseconds", 1000);
                Thread.sleep(1000);
                log.info("Attempting to reconnect to ThingsBoard.");
                tbClient.connect();
                if (tbClient.isConnected()) {
                    log.info("Successfully reconnected to ThingsBoard.");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            return false;
        }
        return true;
    }

    private void clearOutgoingQueue() {
        outgoingQueue.forEach(future -> {
            try {
                future.cancel(true);
            } catch (CancellationException e) {
                log.warn("Failed to cancel outgoing message on disconnected client. Reason: " + e.getMessage(), e);
            }
        });
        outgoingQueue.clear();
    }

    private List<MqttPersistentMessage> getMessages() {
        try {
            List<MqttPersistentMessage> resendMessages = persistentFileService.getResendMessages();
            if (!resendMessages.isEmpty()) {
                return resendMessages;
            } else {
                return persistentFileService.getPersistentMessages();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}

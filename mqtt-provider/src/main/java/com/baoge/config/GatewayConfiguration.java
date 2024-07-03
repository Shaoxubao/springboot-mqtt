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
package com.baoge.config;

import com.baoge.gateway.GatewayService;
import com.baoge.gateway.MqttGatewayService;
import com.baoge.service.PersistentFileService;
import com.baoge.service.TenantManagerService;
import com.baoge.service.impl.DefaultTenantManagerService;
import com.baoge.service.impl.PersistentFileServiceImpl;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Consumer;

/**
 *
 */
@Configuration
public class GatewayConfiguration {

    public static final int NIO_EVENT_LOOP_GROUP_THREADS = 100;

    @Bean
    public TenantManagerService getTenantManagerService() {
        return new DefaultTenantManagerService() {
            @Override
            public GatewayService getGatewayService() {
                return getGatewayServiceBean();
            }
        };
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public PersistentFileService getPersistentFileServiceBean(String tenantName) {
        PersistentFileServiceImpl persistentFileService = new PersistentFileServiceImpl();
        persistentFileService.setTenantName(tenantName);
        return persistentFileService;
    }


    @Bean
//    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public GatewayService getGatewayServiceBean() {
        MqttGatewayService gatewayService = new MqttGatewayService();
        gatewayService.setPersistentFileService(getPersistentFileServiceBean("Tenant"));
        return gatewayService;
    }

    @Bean
    public NioEventLoopGroup getNioEventLoopGroupBean() {
        return new NioEventLoopGroup(NIO_EVENT_LOOP_GROUP_THREADS);
    }

}

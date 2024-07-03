package com.baoge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mqtt 生产者
 *
 */

@SpringBootApplication
public class MqttProviderApplication {
    public static void main( String[] args ) {
        SpringApplication.run(MqttProviderApplication.class, args);
    }
}

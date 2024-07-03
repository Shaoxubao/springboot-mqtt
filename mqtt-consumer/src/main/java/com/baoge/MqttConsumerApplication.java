package com.baoge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mqtt 消费者
 *
 */

@SpringBootApplication
public class MqttConsumerApplication {
    public static void main( String[] args ) {
        SpringApplication.run(MqttConsumerApplication.class, args);
    }
}

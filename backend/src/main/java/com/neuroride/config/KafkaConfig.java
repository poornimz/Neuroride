package com.neuroride.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic driverLocationTopic(@Value("${neuroride.kafka.topics.driver-location}") String name) {
        return TopicBuilder.name(name).partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic rideRequestsTopic(@Value("${neuroride.kafka.topics.ride-requests}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tripCompletedTopic(@Value("${neuroride.kafka.topics.trip-completed}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
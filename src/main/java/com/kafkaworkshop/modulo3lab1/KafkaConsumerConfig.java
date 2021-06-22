package com.kafkaworkshop.modulo3lab1;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    public ConsumerFactory<String, String> consumerFactory(String groupId) {
        Map<String, Object> configProps = new HashMap<>();
        String bootstrapAddress = System.getenv("BOOTSTRAP_ADDRESS");
        String trustStoreLocation = System.getenv("TRUSTED_STORE_LOCATION");
        String trustStorePassword = System.getenv("TRUSTED_STORE_PASSWORD");
        String keyStoreLocation = System.getenv("KEY_STORE_LOCATION");
        String keyStorePassword = System.getenv("KEY_STORE_PASSWORD");
        String keyPassword = System.getenv("KEY_PASSWORD");
        
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put("security.protocol", "SSL");
        configProps.put("ssl.truststore.location", trustStoreLocation);
        configProps.put("ssl.truststore.password", trustStorePassword);
        configProps.put("ssl.key.password", keyPassword);
        configProps.put("ssl.keystore.password", keyStorePassword);
        configProps.put("ssl.keystore.location", keyStoreLocation);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(String groupId) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(groupId));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> module3lab1KafkaListenerContainerFactory() {
        return kafkaListenerContainerFactory("module3lab1");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> filterKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = kafkaListenerContainerFactory("filter");
        factory.setRecordFilterStrategy(record -> record.value()
            .contains("World"));
        return factory;
    }
}

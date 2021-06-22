package com.kafkaworkshop.modulo3lab1;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@EnableKafka
@Configuration
public class KafkaProducerConfig {

  @Bean
  public ProducerFactory<String, String> producerFactory() {
    String bootstrapAddress = System.getenv("BOOTSTRAP_ADDRESS");
    String trustStoreLocation = System.getenv("TRUSTED_STORE_LOCATION");
    String trustStorePassword = System.getenv("TRUSTED_STORE_PASSWORD");
    String keyStoreLocation = System.getenv("KEY_STORE_LOCATION");
    String keyStorePassword = System.getenv("KEY_STORE_PASSWORD");
    String keyPassword = System.getenv("KEY_PASSWORD");

    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put("security.protocol", "SSL");
    configProps.put("ssl.truststore.location", trustStoreLocation);
    configProps.put("ssl.truststore.password", trustStorePassword);
    configProps.put("ssl.key.password", keyPassword);
    configProps.put("ssl.keystore.password", keyStorePassword);
    configProps.put("ssl.keystore.location", keyStoreLocation);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}

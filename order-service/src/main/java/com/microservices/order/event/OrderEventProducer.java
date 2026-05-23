package com.microservices.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic:order-topic}")
    private String topic;

    public void publish(OrderEvent event) {
        log.info("Publicando evento de pedido en topic {}: {}", topic, event);
        try {
            kafkaTemplate.send(topic, event).get(10, TimeUnit.SECONDS);
            log.info("Evento de pedido publicado exitosamente");
        } catch (Exception e) {
            log.error("Error al publicar evento de pedido: {}", e.getMessage(), e);
            throw new RuntimeException("Error al publicar evento en Kafka", e);
        }
    }
}

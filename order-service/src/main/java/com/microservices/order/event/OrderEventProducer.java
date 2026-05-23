package com.microservices.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic:order-topic}")
    private String topic;

    public void publish(OrderEvent event) {
        log.info("Publicando evento de pedido en topic {}: {}", topic, event);
        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Evento de pedido publicado exitosamente. Offset: {}",
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Error al publicar evento de pedido: {}", ex.getMessage(), ex);
                    }
                });
    }
}

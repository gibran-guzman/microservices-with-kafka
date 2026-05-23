package com.microservices.notification.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventConsumer {

    @KafkaListener(
            topics = "${app.kafka.topic:order-topic}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(OrderEvent event) {
        log.info("========================================");
        log.info("Pedido recibido correctamente");
        log.info("OrderId: {}", event.getOrderId());
        log.info("CustomerId: {}", event.getCustomerId());
        log.info("ProductId: {}", event.getProductId());
        log.info("Quantity: {}", event.getQuantity());
        log.info("Timestamp: {}", event.getTimestamp());
        log.info("========================================");
    }
}

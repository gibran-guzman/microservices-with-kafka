package com.microservices.notification.event;

import com.microservices.notification.entity.ProcessedEvent;
import com.microservices.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "${app.kafka.topic:order-topic}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(OrderEvent event) {
        if (event == null) {
            log.warn("Evento nulo recibido, ignorando");
            return;
        }
        if (event.getOrderId() == null) {
            log.warn("Evento sin orderId, ignorando");
            return;
        }

        String eventKey = "order-" + event.getOrderId();
        if (processedEventRepository.existsByEventKey(eventKey)) {
            log.info("Evento duplicado ignorado para orderId: {}", event.getOrderId());
            return;
        }

        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventKey(eventKey)
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Evento duplicado ignorado (race condition) para orderId: {}", event.getOrderId());
            return;
        }

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

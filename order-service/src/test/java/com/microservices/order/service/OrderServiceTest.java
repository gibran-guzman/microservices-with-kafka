package com.microservices.order.service;

import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.order.dto.OrderRequest;
import com.microservices.order.dto.OrderResponse;
import com.microservices.order.entity.Order;
import com.microservices.order.event.OrderEventProducer;
import com.microservices.order.mapper.OrderMapper;
import com.microservices.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;
    @Mock
    private OrderMapper mapper;
    @Mock
    private OrderEventProducer eventProducer;
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OrderService service;

    @BeforeEach
    void setUp() {
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        service = new OrderService(repository, mapper, eventProducer, restClientBuilder);
    }

    @Test
    void create_shouldReturnOrderResponse() {
        OrderRequest request = new OrderRequest(1L, 1L, 2);
        Order entity = Order.builder().customerId(1L).productId(1L).quantity(2).build();
        Order saved = Order.builder().id(1L).customerId(1L).productId(1L).quantity(2).build();
        OrderResponse response = OrderResponse.builder().id(1L).customerId(1L).productId(1L).quantity(2).build();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("stock", 10, "precio", 50.0));
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        OrderResponse result = service.create(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCustomerId()).isEqualTo(1L);
        verify(eventProducer).publish(any());
    }

    @Test
    void create_shouldThrowResourceNotFoundException_whenCustomerNotFound() {
        OrderRequest request = new OrderRequest(99L, 1L, 2);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_shouldThrowIllegalArgumentException_whenInsufficientStock() {
        OrderRequest request = new OrderRequest(1L, 1L, 50);
        Order entity = Order.builder().customerId(1L).productId(1L).quantity(50).build();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("stock", 5));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void findById_shouldReturnOrderResponse() {
        Order order = Order.builder().id(1L).customerId(1L).productId(1L).quantity(2).build();
        OrderResponse response = OrderResponse.builder().id(1L).customerId(1L).productId(1L).quantity(2).build();

        when(repository.findById(1L)).thenReturn(Optional.of(order));
        when(mapper.toResponse(order)).thenReturn(response);

        OrderResponse result = service.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_shouldReturnAllOrders() {
        Order o1 = Order.builder().id(1L).build();
        Order o2 = Order.builder().id(2L).build();
        when(repository.findAll()).thenReturn(List.of(o1, o2));
        when(mapper.toResponse(o1)).thenReturn(OrderResponse.builder().id(1L).build());
        when(mapper.toResponse(o2)).thenReturn(OrderResponse.builder().id(2L).build());

        List<OrderResponse> result = service.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void delete_shouldDeleteOrder() {
        Order order = Order.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(order));

        service.delete(1L);

        verify(repository).delete(order);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

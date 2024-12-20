package com.sparta.msa_exam.order.service;

import com.sparta.msa_exam.order.client.ProductClient;
import com.sparta.msa_exam.order.client.dto.ProductResponseDto;
import com.sparta.msa_exam.order.dto.OrderRequestDto;
import com.sparta.msa_exam.order.dto.OrderResponseDto;
import com.sparta.msa_exam.order.dto.OrderSearchDto;
import com.sparta.msa_exam.order.entity.Order;
import com.sparta.msa_exam.order.enums.OrderStatus;
import com.sparta.msa_exam.order.exception.ProductServiceUnavailableException;
import com.sparta.msa_exam.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    @CachePut(cacheNames = "orderCache", key = "#result.orderId")
    public OrderResponseDto createOrder(OrderRequestDto requestDto, String userId) {
        for (Long productId : requestDto.getOrderItemIds()) {
            ProductResponseDto product = productClient.getProduct(productId);
            if (product.getQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product with ID " + productId + " is out of stock.");
            }
        }

        for (Long productId : requestDto.getOrderItemIds()) {
            try {
                productClient.reduceProductQuantity(productId, 1);
            } catch (Exception e) {
                log.error("Failed to reduce product quantity for product ID {}: {}", productId, e.getMessage());
                throw new ProductServiceUnavailableException("잠시 후에 주문 추가를 요청 해주세요.");
            }
        }

        Order order = Order.builder()
                .orderItemIds(requestDto.getOrderItemIds())
                .createdBy(userId)
                .status(OrderStatus.CREATED)
                .build();

        Order savedOrder = orderRepository.save(order);

        return toResponseDto(savedOrder);
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackGetProductDetails")
    public OrderResponseDto createOrderFailCase(OrderRequestDto orderRequestDto, boolean isFail) {
        if (isFail) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }

        return new OrderResponseDto(
                orderRequestDto.getStatus(),
                orderRequestDto.getOrderItemIds()
        );
    }

    public OrderResponseDto fallbackGetProductDetails(OrderRequestDto orderRequestDto, boolean isFail, Throwable t) {
        throw new ProductServiceUnavailableException("잠시 후에 주문 추가를 요청 해주세요.");
    }

    @Cacheable(value = "orderSearchCache", keyGenerator = "customCacheKeyGenerator")
    public Page<OrderResponseDto> getOrders(OrderSearchDto searchDto, Pageable pageable, String role, String userId) {
        return orderRepository.searchOrders(searchDto, pageable, role, userId);
    }

    @Cacheable(cacheNames = "orderCache", key = "#orderId")
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = findOrderById(orderId);
        return toResponseDto(order);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "orderSearchCache", allEntries = true)
    }, put = {
            @CachePut(cacheNames = "orderCache", key = "args[0]")
    })
    @Transactional
    public OrderResponseDto updateOrder(Long orderId, Long productId, String userId) {
        ProductResponseDto product = productClient.getProduct(productId);
        if (product.getQuantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product with ID " + productId + " is out of stock.");
        }

        Order order = findOrderById(orderId);
        order.updateOrder(productId, userId);
        Order updatedOrder = orderRepository.save(order);
        return toResponseDto(updatedOrder);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "orderCache", key = "#orderId"),
            @CacheEvict(cacheNames = "orderSearchCache", allEntries = true)
    })
    @Transactional
    public void deleteOrder(Long orderId, String deletedBy) {
        Order order = findOrderById(orderId);
        order.deleteOrder(deletedBy);
        orderRepository.save(order);
    }

    private OrderResponseDto toResponseDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getStatus().toString(),
                order.getCreatedAt(),
                order.getCreatedBy(),
                order.getUpdatedAt(),
                order.getUpdatedBy(),
                order.getOrderItemIds()
        );
    }

    private Order findOrderById(Long productId) {
        return orderRepository.findById(productId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or has been deleted"));
    }
}
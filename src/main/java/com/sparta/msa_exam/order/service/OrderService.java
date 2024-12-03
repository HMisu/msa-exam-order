package com.sparta.msa_exam.order.service;

import com.sparta.msa_exam.order.client.ProductClient;
import com.sparta.msa_exam.order.client.dto.ProductResponseDto;
import com.sparta.msa_exam.order.dto.OrderRequestDto;
import com.sparta.msa_exam.order.dto.OrderResponseDto;
import com.sparta.msa_exam.order.dto.OrderSearchDto;
import com.sparta.msa_exam.order.entity.Order;
import com.sparta.msa_exam.order.enums.OrderStatus;
import com.sparta.msa_exam.order.repository.OrderRepository;
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
            productClient.reduceProductQuantity(productId, 1);
        }

        Order order = Order.builder()
                .orderItemIds(requestDto.getOrderItemIds())
                .createdBy(userId)
                .status(OrderStatus.CREATED)
                .build();

        Order savedOrder = orderRepository.save(order);

        return toResponseDto(savedOrder);
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
        Order order = findOrderById(orderId);
        order.getOrderItemIds().add(productId);
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
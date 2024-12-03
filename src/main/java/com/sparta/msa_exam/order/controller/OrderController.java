package com.sparta.msa_exam.order.controller;

import com.sparta.msa_exam.order.dto.OrderRequestDto;
import com.sparta.msa_exam.order.dto.OrderResponseDto;
import com.sparta.msa_exam.order.dto.OrderSearchDto;
import com.sparta.msa_exam.order.enums.OrderStatus;
import com.sparta.msa_exam.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponseDto createOrder(@RequestBody OrderRequestDto orderRequestDto,
                                        @RequestHeader(value = "X-User-Id", required = true) String userId,
                                        @RequestHeader(value = "X-Role", required = true) String role) {

        return orderService.createOrder(orderRequestDto, userId);
    }

    @GetMapping
    public Page<OrderResponseDto> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) List<Long> orderItemIds,
            Pageable pageable,
            @RequestHeader(value = "X-User-Id", required = true) String userId,
            @RequestHeader(value = "X-Role", required = true) String role) {
        if (!"MANAGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. User role is not MANAGER.");
        }
        OrderSearchDto searchDto = new OrderSearchDto(status, orderItemIds);
        return orderService.getOrders(searchDto, pageable, role, userId);
    }

    @GetMapping("/{orderId}")
    public OrderResponseDto getOrderById(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @PutMapping("/{orderId}")
    public OrderResponseDto updateOrder(@PathVariable Long orderId,
                                        @RequestBody Map<String, Long> requestBody,
                                        @RequestHeader(value = "X-User-Id", required = true) String userId,
                                        @RequestHeader(value = "X-Role", required = true) String role) {
        Long productId = requestBody.get("product_id");
        return orderService.updateOrder(orderId, productId, userId);
    }

    @DeleteMapping("/{orderId}")
    public void deleteOrder(@PathVariable Long orderId, @RequestParam String deletedBy) {
        orderService.deleteOrder(orderId, deletedBy);
    }
}

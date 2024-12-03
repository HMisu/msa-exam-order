package com.sparta.msa_exam.order.repository;

import com.sparta.msa_exam.order.dto.OrderResponseDto;
import com.sparta.msa_exam.order.dto.OrderSearchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {
    Page<OrderResponseDto> searchOrders(OrderSearchDto searchDto, Pageable pageable, String role, String userId);
}

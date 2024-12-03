package com.sparta.msa_exam.order.dto;

import com.sparta.msa_exam.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderSearchDto {
    private OrderStatus status;
    private List<Long> orderItemIds;
}
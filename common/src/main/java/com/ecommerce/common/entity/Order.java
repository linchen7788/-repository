package com.ecommerce.common.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Long orderId;
    private String userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Date createTime;

    public enum OrderStatus {
        CREATED,
        PAID,
        SHIPPED,
        COMPLETED,
        CANCELLED
    }
}
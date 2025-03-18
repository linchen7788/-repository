package com.ecommerce.order.service;

import com.ecommerce.common.entity.Order;

public interface OrderService {
    Boolean createOrderWithLock(Order order);

    void orderService.updateOrderStatus(Order order);
}
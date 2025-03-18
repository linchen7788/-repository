package com.ecommerce.order.controller;

import com.ecommerce.common.entity.Order;
import com.ecommerce.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Boolean> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrderWithLock(order));
    }
}
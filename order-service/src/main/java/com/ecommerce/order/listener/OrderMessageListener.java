package com.ecommerce.order.listener;

import com.ecommerce.common.entity.Order;
import com.ecommerce.order.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "ORDER_CREATED_TOPIC",
    consumerGroup = "order-consumer-group",
    selectorExpression = "*"
)
public class OrderMessageListener implements RocketMQListener<Order> {

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(Order order) {
        // 三阶段提交事务协调
        String txId = generateTransactionId();
        TransactionLog prepareLog = new TransactionLog(txId, TransactionStatus.PREPARE, order.getId());
        transactionLogRepository.save(prepareLog);

        try {
            // 阶段一：准备阶段（订单信息校验、库存预扣减）
            // 1.校验订单基本信息有效性
            // 2.调用库存服务预扣减库存
            // 3.生成预占用的优惠券记录
            boolean prepared = orderService.prepareTransaction(order);
            if (!prepared) {
                handleRollback(txId, order);
                return;
            }

            // 阶段二：预提交阶段（支付预授权、物流预分配）
            // 1.调用支付服务进行预授权
            // 2.调用物流服务分配运单号
            // 3.生成预占用的积分记录
            TransactionLog preCommitLog = new TransactionLog(txId, TransactionStatus.COMMIT, order.getId());
            transactionLogRepository.save(preCommitLog);
            if (!orderService.preCommitTransaction(order)) {
                handleRollback(txId, order);
                return;
            }

            // 阶段三：提交阶段（正式扣减库存、生成物流单）
            // 1.正式扣减商品库存
            // 2.生成正式物流单
            // 3.更新优惠券/积分使用状态
            orderService.commitTransaction(order);
            transactionLogRepository.save(new TransactionLog(txId, TransactionStatus.COMMIT, order.getId()));

        } catch (Exception e) {
            handleRollback(txId, order);
        } finally {
            scheduleTimeoutCheck(txId, order);
        }
    }
}
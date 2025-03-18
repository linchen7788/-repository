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
            // 阶段一：准备阶段
            boolean prepared = orderService.prepareTransaction(order);
            if (!prepared) {
                handleRollback(txId, order);
                return;
            }

            // 阶段二：预提交阶段
            TransactionLog preCommitLog = new TransactionLog(txId, TransactionStatus.COMMIT, order.getId());
            transactionLogRepository.save(preCommitLog);
            if (!orderService.preCommitTransaction(order)) {
                handleRollback(txId, order);
                return;
            }

            // 阶段三：提交阶段
            orderService.commitTransaction(order);
            transactionLogRepository.save(new TransactionLog(txId, TransactionStatus.COMMIT, order.getId()));

        } catch (Exception e) {
            handleRollback(txId, order);
        } finally {
            scheduleTimeoutCheck(txId, order);
        }
     }
}
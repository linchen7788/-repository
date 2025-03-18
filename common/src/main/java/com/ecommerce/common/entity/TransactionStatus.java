package com.ecommerce.common.entity;
/**
 * 事务状态枚举和事务日志实体类
 */
public enum TransactionStatus {
    PREPARE,
    COMMIT,
    ROLLBACK,
    TIMEOUT
}
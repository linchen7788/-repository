package com.ecommerce.common.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

import java.util.Date;

@Entity
@Data
public class TransactionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String transactionId;
    private TransactionStatus status;
    private Date createTime;
    private String phase;
    private Long orderId;
    private Date timeout;

    // 省略getter/setter和构造方法

}
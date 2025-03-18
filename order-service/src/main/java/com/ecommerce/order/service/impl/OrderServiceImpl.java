package com.ecommerce.order.service.impl;

import com.ecommerce.common.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
/**
 * 订单服务实现类
 * 使用Redis分布式锁保证订单创建操作的原子性
 */
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 分布式锁键前缀（格式：order_lock:{用户ID}）
    private static final String LOCK_PREFIX = "order_lock:";
    // 锁过期时间（单位：秒）
    private static final int LOCK_EXPIRE = 30;

    @Override
    /**
     * 创建订单（带分布式锁）
     * @param order 订单实体
     * @return 创建成功返回true，获取锁失败返回false
     */
    @Override
    public Boolean createOrderWithLock(Order order) {
        // 生成分布式锁Key（用户维度锁）
        String lockKey = LOCK_PREFIX + order.getUserId();
        String lockValue = UUID.randomUUID().toString();

        try {
            // 尝试获取分布式锁（SETNX操作）
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_EXPIRE, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                // 实际订单创建逻辑
                // 核心业务逻辑：持久化订单数据
                orderMapper.insert(order);
                return true;
            }
            return false;
        } finally {
            // Lua脚本保证原子性删除（仅当锁值与当前持有者匹配时才删除）
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(lockKey), lockValue);
        }
    }


    @Override
    @Transactional
    public void updateOrderStatus(Order order) {
        // 查询当前订单状态
        OrderStatus currentStatus = orderMapper.selectStatusById(order.getId());
        
        // 校验状态转换有效性
        if (!OrderStatus.isValidTransition(currentStatus, order.getStatus())) {
            throw new IllegalStateException("非法状态转换：从 " + currentStatus + " 到 " + order.getStatus());
        }
        
        // 更新状态并记录操作时间
        orderMapper.updateStatus(order.getId(), order.getStatus(), LocalDateTime.now());
    }
}
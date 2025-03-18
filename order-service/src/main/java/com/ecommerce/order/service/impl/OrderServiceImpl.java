package com.ecommerce.order.service;

import com.ecommerce.common.entity.Order;
import com.ecommerce.common.entity.TransactionLog;
import com.ecommerce.common.enums.TransactionStatus;
import com.ecommerce.order.feign.InventoryServiceClient;
import com.ecommerce.order.feign.PaymentServiceClient;
import com.ecommerce.order.feign.LogisticsServiceClient;
import com.ecommerce.order.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分布式事务三阶段提交实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private InventoryServiceClient inventoryService;

    @Autowired
    private PaymentServiceClient paymentService;

    @Autowired
    private LogisticsServiceClient logisticsService;

    @Autowired
    private TransactionLogRepository transactionLogRepository;


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


    /**
     * 准备阶段（订单校验 + 库存预扣减）
     * 1.校验订单基础信息（金额、商品有效性）
     * 2.调用库存服务预扣减库存（生成预占库存记录）
     * 3.生成预占用优惠券/积分记录
     */
    @Override
    public boolean prepareTransaction(Order order) {
        // 1.订单基础校验（事务状态：PREPARE）
        // - 校验订单金额是否为正数
        // - 验证商品ID有效性
        // - 检查用户账户状态
        validateOrderBasic(order);

        // 2.库存预扣减（生成预占库存记录）
        // 调用库存服务Feign接口：/inventory/pre-deduct
        // 参数说明：
        //   productId  -> 商品ID（来自订单对象）
        //   quantity   -> 购买数量（来自订单对象）
        String preDeductNo = inventoryService.preDeductStock(
            order.getProductId(), 
            order.getQuantity()
        );
        order.setPreDeductNo(preDeductNo);

        // 3.生成预占用记录（事务日志记录）
        // - 创建优惠券预占用记录（coupon_service.prefreeze）
        // - 生成积分预扣除记录（point_service.prefreeze）
        createPreOccupiedRecords(order);

        return preDeductNo != null;
    }

    /**
     * 预提交阶段（支付预授权 + 物流预分配）
     * 1.调用支付服务进行预授权（冻结资金）
     * 2.调用物流服务分配预运单号
     * 3.生成预授权支付凭证
     */
    @Override
    public boolean preCommitTransaction(Order order) {
        // 1.支付预授权（事务状态：COMMIT）
        // 计算公式：冻结金额 = 订单总金额 - 优惠金额
        // 调用支付服务Feign接口：/payment/pre-authorize
        // 参数说明：
        //   orderNo        -> 订单编号（幂等键）
        //   freezeAmount   -> 计算后的预授权金额
        boolean authResult = paymentService.preAuthorize(
            order.getOrderNo(),
            order.getTotalAmount().subtract(order.getDiscountAmount())
        );

        // 2.物流预分配（生成预运单）
        // 调用物流服务Feign接口：/logistics/pre-create
        // 参数说明：
        //   address -> 包含省市区详细信息的地址对象
        String preWaybillNo = logisticsService.preCreateWaybill(
            order.getAddress()
        );
        order.setPreWaybillNo(preWaybillNo);

        return authResult && preWaybillNo != null;
    }

    /**
     * 正式提交阶段（库存扣减 + 物流单生成）
     * 1.实际扣减库存（根据预扣减流水号）
     * 2.生成正式物流单
     * 3.更新优惠券/积分状态
     */
    @Override
    public void commitTransaction(Order order) {
        // 1.正式库存扣减（事务状态：COMMIT）
        // 调用库存服务Feign接口：/inventory/confirm-deduct
        // 参数说明：
        //   preDeductNo -> 预扣减阶段返回的流水号
        inventoryService.confirmDeductStock(order.getPreDeductNo());

        // 2.生成正式物流单
        // 调用物流服务Feign接口：/logistics/confirm-waybill
        // 参数说明：
        //   preWaybillNo   -> 预提交阶段返回的预运单号
        //   productDetails -> 商品详情（用于生成面单）
        logisticsService.confirmWaybill(
            order.getPreWaybillNo(),
            order.getProductDetails()
        );

        // 3.更新优惠状态（事务完成）
        // - 实际扣除优惠券（coupon_service.confirm）
        // - 正式扣除积分（point_service.confirm）
        updateCouponStatus(order);
    }

    // 其他辅助方法...
}
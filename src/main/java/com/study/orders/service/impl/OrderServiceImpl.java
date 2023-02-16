package com.study.orders.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.study.orders.mapper.OrdersMapper;
import com.study.orders.model.Orders;
import com.study.orders.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;

@Service
public class OrderServiceImpl implements OrderService {
    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addSecKillOrder(Orders orders) {
        try {
            orders.setStatus(1);
            orders.setBuyNum(1);
            orders.setCreateTime(new Date());
            // 订单金额是购买单价乘上购买数量
            orders.setOrderMoney(orders.getBuyPrice().multiply(new BigDecimal(orders.getBuyNum())));
            /**
             * 添加订单数据到数据库，这里可能会抛出异常DuplicateKeyException，表示违反唯一约束异常，一旦抛出这个异常，
             * 表示当前消息是重复消息，阻止异常后，消息会自动出队
             */
            ordersMapper.insert(orders);

            /**
             * 将订单结果写入Redis用于通知前端进行支付
             * 注意：
             *      这个数据需要指定超时时间
             */
            stringRedisTemplate.opsForValue().set("ORDERS_RESULT:" + orders.getGoodsId() + ":" + orders.getUid(),
                    JSONObject.toJSONString(orders), Duration.ofSeconds(60*5));
        } catch (DuplicateKeyException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取订单支付结果
     * @param goodsId
     * @param uid
     * @return
     */
    @Override
    public Orders getOrderResult(Integer goodsId, Integer uid) {
        String orderStr = stringRedisTemplate.opsForValue().get("ORDERS_RESULT:" + goodsId + ":" + uid);
        if (orderStr == null) {
            return null;
        }
        return JSONObject.parseObject(orderStr, Orders.class);
    }
}

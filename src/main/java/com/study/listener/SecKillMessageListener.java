package com.study.listener;

import com.alibaba.fastjson2.JSONObject;
import com.study.orders.model.Orders;
import com.study.orders.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class SecKillMessageListener {
    @Resource
    private OrderService orderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 监听MQ中的消息，将消息出队列
     * @param message
     */
    @RabbitListener(queues = {"secKillQueue"})
    public void getMessage(String message) {
        // 将JSON转换成Java对象
        Orders orders = JSONObject.parseObject(message, Orders.class);
        // 数据库下单
        orderService.addSecKillOrder(orders);
        // 程序到这没有出现任何异常，则表示完成数据库下单，需要将订单数据从Redis中移除掉
        // 如果orderService.addSecKillOrder(orders);中有重复的消息，捕获了DuplicateKeyException异常，此时数据库里肯定也有了
        // 对应的订单数据才会抛出此异常，所以此时也可以将Redis中的备份订单数据删除
        // 在往MQ发消息时，orderJson作为Message，在往Redis存入订单备份数据时，是orderJson作为value， 而Key统一为orders
        // 所以此时将Redis中value为orderJson的数据删除就可以了
        stringRedisTemplate.opsForZSet().remove("orders", message);
    }
}

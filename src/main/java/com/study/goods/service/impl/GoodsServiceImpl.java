package com.study.goods.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.study.goods.mapper.GoodsMapper;
import com.study.goods.model.Goods;
import com.study.goods.service.GoodsService;
import com.study.orders.model.Orders;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoodsServiceImpl implements GoodsService {
    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AmqpTemplate amqpTemplate;
    /**
     * 获取秒杀商品列表
     * 实际中应该根据系统时间获取即将开始或正开始的商品
     * @return
     */
    @Override
    public List<Goods> getGoodsList() {
        return goodsMapper.selectAll();
    }

    @Override
    public Goods getGoodsById(Integer id) {
        return goodsMapper.selectByPrimaryKey(id);
    }

    /**
     * 主业务逻辑方法，需要完成减少库存，添加订单，限购 防超卖，由于并发量比较高不能操作数据库
     * 需要利用redis以及MQ完成，在此之前需要把所有商品信息初始化到Redis中，这样才能通过Redis来记录商品库存变化，完成秒杀方法
     * @param goodsId 商品ID
     * @param randomName 随机名称
     * @param price 价格
     * @param uid 用户id
     * @return 下单结果
     * 0表示下单成功
     * 1表示商品信息异常（随机名不存在）
     * 2表示没有库存
     * 3表示重复购买
     */
    @Override
    public int secKill(Integer goodsId, String randomName, BigDecimal price, Integer uid) {
        /**
         * 定义订单对象，转换成JSON后，存入MQ通知订单模块完成下单，存入Redis防止掉单
         */
        Orders orders = new Orders();
        orders.setUid(uid);
        orders.setGoodsId(goodsId);
        orders.setBuyPrice(price);

        String orderJson = JSONObject.toJSONString(orders);
        // 用户秒杀，商品减库存有可能失败，比如可能逻辑错误（可能别人比较快、在目前用户修改库存之前就修改了商品库存）
        // 所以需要一个死循环一直尝试去秒杀减库存，直到减库存成功，或者出现逻辑错误退出
        while (true) {
            /**
             * 重写execute方法，用于执行多个Redis命令和事务
             * 参数为回调对象，利用这个回调对象中的回调方法来执行多个Redis命令和事务
             * 返回值为这些Redis命令的执行结果
             * 我们的业务返回值有2种类型
             * 1、Integer类型表示出现了逻辑错误
             * 2、返回List集合，如果长度大于0，则表示实物提交成功否则表示实物提交失败
             */
            Object result = stringRedisTemplate.execute(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                    // 定义List集合用于存放所有需要监控的Key，这里需要监控商品库存，防止超卖，需要用户的购买记录，防止重复购买
                    List keys = new ArrayList<>();
                    // 以下放入的库存数据是在定时任务将商品信息初始化到Redis中时，放进去的数据
                    keys.add("GOODS_STORE:" + randomName);
                    keys.add("BUY_RECORD:"+goodsId + ":" + uid);
                    // 复习：watch的作用，监视一个或多个Key，如果在事务的执行之前，这个Key的值被修改，则事务被打断
                    operations.watch(keys);
                    // 获取库存
                    String store = (String)operations.opsForValue().get("GOODS_STORE:" + randomName);
                    // 出现一下错误退出程序之前，都需要释放key监控，表示用户本轮不能再参与秒杀，也就不用再监控key了
                    // 进入if表示商品在Redis中不存在，随机名错误
                    if (store == null) {
                        // 释放key监控
                        operations.unwatch();
                        return 1;
                    }
                    // 进入if表示商品没有库存
                    if (Integer.valueOf(store) <= 0) {
                        operations.unwatch();
                        return 2;
                    }
                    String buyRecord = (String) operations.opsForValue().get("BUY_RECORD:"+goodsId + ":" + uid);
                    // 进入if表示用户有购买记录
                    if (buyRecord != null) {
                        operations.unwatch();
                        return 3;
                    }
                    // 程序到这，暂时没有购买记录，有库存，但不一定能抢到

                    // 开启事务
                    operations.multi();
                    // 减库存
                    operations.opsForValue().decrement((K)("GOODS_STORE:" + randomName));
                    // 使用统一Key前缀+商品id+用户id作为Key，使用任意非null的数据作为value
                    // 添加购买记录
                    operations.opsForValue().set((K)("BUY_RECORD:"+goodsId + ":" + uid), ((V) "1"));

                    /**
                     * 使用固定Key，使用订单JSON作为value，使用系统时间毫秒值作为分数将订单数据存入Redis防止掉单
                     * 后期需要配合定时任务定期扫描判断订单是否存在掉单行为，如果存在掉单行为则将订单补发到MQ中
                     * 这个Redis的订单备份数据只能在完成数据库下单之后才会从Redis删除
                     */
                    operations.opsForZSet().add((K) "orders", (V) orderJson, System.currentTimeMillis());
                    // 提交事务,返回List集合，如果List集合长度大于0表示提交成功，如果集合为null或长度等于0表示事务提交失败
                    // 原因是有其他线程对监控的数据进行了修改
                    return operations.exec();
                }
            });
            // 进入if表示出现逻辑错误
            if (result instanceof Integer) {
                return (int) result;
            }
            List list = (List) result;
            // 进入if表示减库存成功，结束死循环
            if (list != null && !list.isEmpty()) {
                break;
            }
        }
        // 程序到这表示成功减少库存，添加了购买记录

        // 订单JSON存入MQ，通知订单模块完成下单
        amqpTemplate.convertAndSend("secKillExchange", "", orderJson);
        return 0;
    }
}

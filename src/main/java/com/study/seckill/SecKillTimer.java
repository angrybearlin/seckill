package com.study.seckill;

import com.study.goods.model.Goods;
import com.study.goods.service.GoodsService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@EnableScheduling
public class SecKillTimer {
    @Resource
    private GoodsService goodsService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AmqpTemplate amqpTemplate;

    /**
     * 配置定时任务，每秒钟执行一次，将秒杀的商品信息初始化到Redis中
     * 实际项目，应该是固定的时间执行例如每天的23：55将未来一天的商品初始化到Redis中，或在活动即将开始的时候初始化到Redis中
     * 初始化时应该根据系统时间将即将开始活动的商品初始化到Redis中
     */
    @Scheduled(cron = "* * * * * *")
    public void initSecKillDataToRedis() {
        // 获取所有秒杀商品，实际项目应该根据系统时间获取即将开始活动的商品
        List<Goods> goodsList = goodsService.getGoodsList();
        goodsList.forEach(goods -> {
            // 使用统一key前缀+商品随机名作为key，商品库存为value,将数据初始化到Redis中
            // setIfAbsent 方法插入数据时，如果key存在则放弃插入，不存在则插入数据
            stringRedisTemplate.opsForValue().setIfAbsent("GOODS_STORE:" + goods.getRandomName(), goods.getStore() + "");
        });
    }

    /**
     * 配置定时任务，每5秒钟执行一次，读取Redis中的订单备份数据
     * 实际不能每5秒钟执行一次，应该是5分钟或10分钟一次
     */
    @Scheduled(cron = "0/5 * * * * *")
    public void diaoDan() {
        // 计算当前系统时间5分钟之前的系统毫秒值
        long maxScore = System.currentTimeMillis()-1000*60*5;
        /**
         * 使用0作为开始分数，使用当前系统时间5分钟之前的时间毫秒值作为最大分数，读取Redis中订单备份数据中的数据
         * 这些订单数据可能存在掉单行为，将这些订单数据补发到MQ中
         * 这样读取到的就是五分钟之前以及更久远，产生的所有订单备份数据，因为是定时任务查询这些数据，所以不用查询此刻之前产生的所有订单备份数据
         */
        Set<String> orderSet = stringRedisTemplate.opsForZSet().rangeByScore("orders", 0, maxScore);
        // 循环可能掉单的订单数据，补发到MQ中。此时发送的是所有的订单数据，不管有没有掉单都会发
        orderSet.forEach(orderStr -> {
            amqpTemplate.convertAndSend("secKillExchange", "", orderStr);
        });
    }
}

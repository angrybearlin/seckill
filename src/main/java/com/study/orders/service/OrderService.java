package com.study.orders.service;

import com.study.orders.model.Orders;

public interface OrderService {
    void addSecKillOrder(Orders orders);

    Orders getOrderResult(Integer goodsId, Integer uid);
}

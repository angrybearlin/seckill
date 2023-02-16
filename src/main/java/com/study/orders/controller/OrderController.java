package com.study.orders.controller;

import com.study.commons.ReturnObject;
import com.study.orders.model.Orders;
import com.study.orders.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
public class OrderController {
    @Resource
    private OrderService orderService;
    @RequestMapping("/getOrderResult")
    @ResponseBody
    public Object getOrderResult(Integer goodsId) {
        Integer uid = 1;
        Orders orders = orderService.getOrderResult(goodsId, uid);
        if (orders == null) {
            return new ReturnObject(1, "暂时没有订单", null);
        }
        return new ReturnObject(0, "获取订单成功", orders);
    }
}

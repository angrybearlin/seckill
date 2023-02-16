package com.study.goods.controller;

import com.study.commons.ReturnObject;
import com.study.goods.model.Goods;
import com.study.goods.service.GoodsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class GoodsController {
    @Resource
    private GoodsService goodsService;

    /**
     * 跳转到商城首页
     * @param model
     * @return
     */
    @RequestMapping("/")
    public String getGoodsList(Model model) {
        List<Goods> goodsList = goodsService.getGoodsList();
        model.addAttribute("goodsList", goodsList);
        return "index";
    }

    /**
     * 展示商品详情
     * @param id
     * @param model
     * @return
     */
    @RequestMapping("/showGoodsInfo")
    public String showGoodsInfo(Integer id, Model model) {
        Goods goods = goodsService.getGoodsById(id);
        model.addAttribute("goods", goods);
        return "goodsInfo";
    }

    /**
     * 秒杀商品
     * @param goodsId
     * @param randomName
     * @param price
     * @return
     */
    @RequestMapping("/secKill")
    @ResponseBody
    public Object secKill(Integer goodsId, String randomName, BigDecimal price) {
        // 当前登录的用户id ，这里写死为1
        Integer uid = 1;
        // 秒杀的主业务方法，返回值为下单结果，例如 0表示下单成功
        int result = goodsService.secKill(goodsId, randomName, price, uid);
        switch (result) {
            case 1:
                return new ReturnObject(1, "商品信息异常", null);
            case 2:
                return new ReturnObject(1, "商品已被抢光", null);
            case 3:
                return new ReturnObject(1, "不能重复购买", null);
        }
        return new ReturnObject(0, "请求成功", null);
    }
}

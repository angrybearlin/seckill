package com.study.goods.service;

import com.study.goods.model.Goods;

import java.math.BigDecimal;
import java.util.List;

public interface GoodsService {
    List<Goods> getGoodsList();

    Goods getGoodsById(Integer id);


    int secKill(Integer goodsId, String randomName, BigDecimal price, Integer uid);
}

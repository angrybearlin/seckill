/**
 * 初始化秒杀用于控制秒杀按钮是否可以点击
 * @param goodsId 商品id
 * @param startTime 活动开始时间
 * @param endTime 活动结束时间
 * @param price 商品价格
 * @param randomName 商品随机名称
 */
function initSecKill(goodsId, startTime, endTime, price, randomName) {
    // 进入页面先将抢购按钮置为不可点击
    $("#secKillBtn").attr("disabled", false);
    var nowTime = new Date().getTime();
    // 判断当前是否大于开始时间，小于结束时间
    // 当前时间小于开始时间
    if (nowTime < startTime) {
        $("#seckillSpan").html("活动没有开始");
        return false;
    }
    // 当前时间大于结束时间
    if (nowTime>endTime) {
        $("#seckillSpan").html("活动已经结束");
        return false;
    }
    // 此时当前时间大于开始时间，小于结束时间，可以进行秒杀
    secKill(goodsId, price, randomName);
}

/**
 * 开始秒杀绑定点击事件,发送秒杀请求
 * @param goodsId
 * @param price
 * @param randomName
 */
function secKill(goodsId, price, randomName) {
    // 设置按钮可用
    $("#secKillBtn").attr("disabled", false);
    // 绑定点击事件，发送秒杀请求
    $("#secKillBtn").bind("click", function () {
        // 点击后设置按钮不可用，防止重复提交购买请求，但是不能百分百拦截重复请求
        $("#secKillBtn").attr("disabled", true);
        // 发送抢购请求
        $.get("/secKill",
            {
                goodsId:goodsId,
                price:price,
                randomName:randomName
            },
            function (data) {
                if (data.code == 1) {
                    alert(data.msg);
                    return false;
                }
                getOrderResult(goodsId);
            },
            "json"
        )
    })
}

/**
 * 轮询获取订单结果显示支付信息
 * @param goodsId
 */
function getOrderResult(goodsId) {
    $.get("/getOrderResult",
        {
            goodsId:goodsId,
        },
        function (data) {
        // 进入if表示请求失败，没有获取到订单结果，需要轮询再次尝试获取
            if (data.code == 1) {
                // 延迟3000毫米后轮询再次尝试获取订单结果
                window.setTimeout("getOrderResult("+goodsId+")", "3000");
                return false;
            }
            var orderMoney = data.result.orderMoney;
            var orderId = data.result.orderId;
            $("#seckillSpan").html("下单成功，共计"+orderMoney+"元&nbsp;&nbsp;&nbsp;&nbsp;<a href='/toPay?orderId="+orderId+"'>立即支付</a>")
        },
        "json"
    )
}

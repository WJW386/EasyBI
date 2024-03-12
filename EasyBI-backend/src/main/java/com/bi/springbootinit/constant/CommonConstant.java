package com.bi.springbootinit.constant;

/**
 * 通用常量
 *
 * @author Willow
 * 
 */
public interface CommonConstant {

    /**
     * 升序
     */
    String SORT_ORDER_ASC = "ascend";

    /**
     * 降序
     */
    String SORT_ORDER_DESC = " descend";

    /**
     * 模型ID
     */
    long MODEL_ID = 1709156902984093697L;

    /**
     * BI-MQ交换机名称
     */
    String BI_EXCHANGE_NAME = "bi_exchange";

    /**
     * BI-MQ队列名称
     */
    String BI_QUEUE_NAME = "bi_queue";

    /**
     * BI-MQ路由键名称
     */
    String BI_ROUTING_KEY = "bi_routingKey";
}

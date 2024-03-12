package com.bi.springbootinit.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.bi.springbootinit.constant.CommonConstant.BI_EXCHANGE_NAME;
import static com.bi.springbootinit.constant.CommonConstant.BI_ROUTING_KEY;

/**
 * @author Willow
 **/
@Component
public class BiMessageProducer {
    @Resource
    RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BI_EXCHANGE_NAME, BI_ROUTING_KEY, message);
    }
}

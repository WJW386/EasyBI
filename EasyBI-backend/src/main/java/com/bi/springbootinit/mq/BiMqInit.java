package com.bi.springbootinit.mq;

import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.exception.BusinessException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.bi.springbootinit.constant.CommonConstant.*;

/**
 * 创建RabbitMQ交换机和队列
 * @author Willow
 **/
public class BiMqInit {
    public static void main(String[] args) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost("localhost");
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(BI_EXCHANGE_NAME, "direct");

            channel.queueDeclare(BI_QUEUE_NAME, true, false,false, null);
            channel.queueBind(BI_QUEUE_NAME, BI_EXCHANGE_NAME,BI_ROUTING_KEY);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建MQ失败");
        }
    }
}

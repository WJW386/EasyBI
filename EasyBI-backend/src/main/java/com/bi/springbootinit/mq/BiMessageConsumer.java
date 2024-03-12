package com.bi.springbootinit.mq;

import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.exception.BusinessException;
import com.bi.springbootinit.exception.ThrowUtils;
import com.bi.springbootinit.manager.AIManager;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.model.enums.ChartStateEnum;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.bi.springbootinit.constant.CommonConstant.BI_QUEUE_NAME;
import static com.bi.springbootinit.constant.CommonConstant.MODEL_ID;


/**
 * @author Willow
 **/
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    ChartService chartService;

    @Resource
    AIManager aiManager;

    @SneakyThrows
    @RabbitListener(queues = {BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Receive Message: {}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long id = Long.parseLong(message);
        Chart chart = chartService.getById(id);
        if (ObjectUtils.isEmpty(chart)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表为空");
        }
        // 先修改图表任务状态为 running，再执行操作
        Chart updateChart = new Chart();
        updateChart.setId(id);
        updateChart.setStatus(ChartStateEnum.RUNNING.getValue());
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            return;
        }
        // 拼接input
        String input = chartService.buildInput(chart);

        String result = aiManager.doChat(MODEL_ID, input);
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(chart.getId(), "AI生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus(ChartStateEnum.SUCCEED.getValue());

        boolean save = chartService.updateById(updateChartResult);
        if (!save) {
            channel.basicNack(deliveryTag, false, false);
            chartService.handleChartUpdateError(chart.getId(), "保存图表到数据库失败");
        }
        channel.basicAck(deliveryTag, false);
    }
}

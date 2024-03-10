package com.bi.springbootinit.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bi.springbootinit.manager.AIManager;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.model.entity.FailedChartInput;
import com.bi.springbootinit.model.enums.ChartStateEnum;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.service.FailedChartInputService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 定时把生成失败的图表放回队列，进行补偿
 *
 * @author Willow
 **/
@Component
@Slf4j
public class AddFailChartToQueueJob {

    @Resource
    ChartService chartService;

    @Resource
    ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private FailedChartInputService failedChartInputService;

    @Resource
    private AIManager aiManager;

    /**
     * 每十分钟执行一次
     */
//    @Scheduled(fixedRate = 600 * 1000)
    public void run() {
        long modelId = 1709156902984093697L;
        // 获取所有标记为失败的任务输入
        QueryWrapper<FailedChartInput> failedChartInputQueryWrapper = new QueryWrapper<>();
        List<FailedChartInput> failedChartsInput = failedChartInputService.list(failedChartInputQueryWrapper);
        if (ObjectUtils.isEmpty(failedChartsInput)) {
            return;
        }
        //
        for (FailedChartInput failedChartInput : failedChartsInput) {
            String input = failedChartInput.getInput();
            Long id = failedChartInput.getId();
            Chart chart = chartService.getById(id);
            if (!chart.getStatus().equals(ChartStateEnum.FAILED.getValue())) {
                failedChartInputService.removeById(id);
                continue;
            }
            try {
                CompletableFuture.runAsync(() -> {
                        // 修改状态为执行中
                        Chart updateChart = new Chart();
                        updateChart.setId(chart.getId());
                        updateChart.setStatus(ChartStateEnum.RUNNING.getValue());
                        boolean updateResult = chartService.updateById(updateChart);
                        if (!updateResult) {
                            chartService.handleChartUpdateError(chart.getId(), "更新图表状态为Running失败", input.toString());
                            return;
                        }
                        // 开始执行任务
                        String result = aiManager.doChat(modelId, input.toString());
                        String[] splits = result.split("【【【【【");
                        if (splits.length < 3) {
                            chartService.handleChartUpdateError(chart.getId(), "AI生成错误", input.toString());
                        }
                        String genChart = splits[1].trim();
                        String genResult = splits[2].trim();

                        // 把执行成功后的结果保存到数据库
                        Chart updateChart2 = new Chart();
                        updateChart2.setId(chart.getId());
                        updateChart2.setStatus(ChartStateEnum.SUCCEED.getValue());
                        updateChart2.setGenChart(genChart);
                        updateChart2.setGenResult(genResult);
                        chartService.updateById(updateChart2);

                        boolean b = failedChartInputService.removeById(id);
//                        if (!b) {
//                            chartService.handleChartUpdateError(chart.getId(), "删除输入表失败", input.toString());
//                        }

                    }, threadPoolExecutor);
            } catch (RejectedExecutionException e) {
                // 处理任务队列满了的情况
                chartService.handleChartUpdateError(chart.getId(), "任务队列已满，无法处理更多任务", input);
            }
        }
    }

}

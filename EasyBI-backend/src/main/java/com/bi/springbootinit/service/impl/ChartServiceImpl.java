package com.bi.springbootinit.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.constant.CommonConstant;
import com.bi.springbootinit.exception.BusinessException;
import com.bi.springbootinit.exception.ThrowUtils;
import com.bi.springbootinit.manager.AIManager;
import com.bi.springbootinit.mapper.ChartMapper;
import com.bi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.model.entity.FailedChartInput;
import com.bi.springbootinit.model.entity.User;
import com.bi.springbootinit.model.enums.ChartStateEnum;
import com.bi.springbootinit.model.vo.BIResponse;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.service.FailedChartInputService;
import com.bi.springbootinit.utils.ExcelUtils;
import com.bi.springbootinit.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Yurio
 * @description 针对表【chart(图表信息表)】的数据库操作Service实现
 * @createDate 2024-03-06 23:27:50
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private AIManager aiManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private FailedChartInputService failedChartInputService;

    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String chartName = chartQueryRequest.getChartName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(chartName), "chart_name", chartName);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chart_type", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq("is_delete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    @Override
    public BIResponse genChartAsync(MultipartFile multipartFile, String chartType, String goal, String chartName, User loginUser, long modelId) {
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() >= 100,
                ErrorCode.PARAMS_ERROR, "表名过长");

        // 文件校验
        final long ONE_MB = 1024 * 1024L;
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> suffixes = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!suffixes.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        // 拼接输入
        StringBuilder input = new StringBuilder();
        input.append("Analysis goal: ");
        String userGoal = goal;
        if (chartType != null) {
            userGoal += "，请使用" + chartType;
        }
        input.append(userGoal).append("\n");
        String data = ExcelUtils.excelToCsv(multipartFile);
        input.append("Raw data:").append("\n");
        input.append(data).append("\n");

        // 先将用户想要执行的任务保存到数据库中
        Chart chart = new Chart();
        chart.setChartName(chartName);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setGoal(goal);
        chart.setStatus(ChartStateEnum.WAIT.getValue());
        boolean save = this.save(chart);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图表到数据库失败");
        }

        // 执行任务
        try {
            CompletableFuture.runAsync(() -> {
                // 修改状态为执行中
                Chart updateChart = new Chart();
                updateChart.setId(chart.getId());
                updateChart.setStatus(ChartStateEnum.RUNNING.getValue());
                boolean updateResult = this.updateById(updateChart);
                if (!updateResult) {
                    handleChartUpdateError(chart.getId(), "更新图表状态为Running失败", input.toString());
                    return;
                }
                // 开始执行任务
                String result = aiManager.doChat(modelId, input.toString());
                String[] splits = result.split("【【【【【");
                if (splits.length < 3) {
                    handleChartUpdateError(chart.getId(), "AI生成错误", input.toString());
                }
                String genChart = splits[1].trim();
                String genResult = splits[2].trim();

                // 把执行成功后的结果保存到数据库
                Chart updateChart2 = new Chart();
                updateChart2.setId(chart.getId());
                updateChart2.setStatus(ChartStateEnum.SUCCEED.getValue());
                updateChart2.setGenChart(genChart);
                updateChart2.setGenResult(genResult);
                this.updateById(updateChart2);

            }, threadPoolExecutor);
        } catch (RejectedExecutionException e) {
            // 处理任务队列满了的情况
            handleChartUpdateError(chart.getId(), "任务队列已满，无法处理更多任务", input.toString());
        }

        BIResponse response = new BIResponse();
        response.setChartId(chart.getId());
        return response;
    }

    @Override
    public BIResponse genChart(MultipartFile multipartFile, String chartType, String goal, String chartName, User loginUser, long modelId) {
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() >= 100,
                ErrorCode.PARAMS_ERROR, "表名过长");

        // 文件校验
        final long ONE_MB = 1024 * 1024L;
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> suffixes = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!suffixes.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        // 拼接输入
        StringBuilder input = new StringBuilder();
        input.append("Analysis goal: ");
        String userGoal = goal;
        if (chartType != null) {
            userGoal += "，请使用" + chartType;
        }
        input.append(userGoal).append("\n");
        String data = ExcelUtils.excelToCsv(multipartFile);
        input.append("Raw data:").append("\n");
        input.append(data).append("\n");
        System.out.println(input);

        String result = aiManager.doChat(modelId, input.toString());
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        Chart chart = new Chart();
        chart.setChartName(chartName);
        chart.setGenChart(genChart);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setGoal(goal);
        chart.setGenResult(genResult);
        chart.setStatus("succeed");
        boolean save = this.save(chart);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图表到数据库失败");
        }

        BIResponse response = new BIResponse();
        response.setChartId(chart.getId());
        response.setGenChart(genChart);
        response.setGenResult(genResult);
        return response;
    }

    @Override
    public void handleChartUpdateError(long chartId, String execMessage, String input) {
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setStatus(ChartStateEnum.FAILED.getValue());
        chart.setExecMessage(execMessage);

        // 把失败图表input存入图表信息表，用于定时任务重试
        FailedChartInput failedChartInput = new FailedChartInput();
        failedChartInput.setId(chartId);
        failedChartInput.setInput(input);

        boolean update = this.updateById(chart);
        if (!update) {
            log.error("更新图表失败状态失败");
        }

        boolean save = failedChartInputService.save(failedChartInput);
        if (!save) {
            log.error("保存图表输入失败");
        }
    }

}





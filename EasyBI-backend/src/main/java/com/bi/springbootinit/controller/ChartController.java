package com.bi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bi.springbootinit.annotation.AuthCheck;
import com.bi.springbootinit.common.BaseResponse;
import com.bi.springbootinit.common.DeleteRequest;
import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.common.ResultUtils;
import com.bi.springbootinit.constant.CommonConstant;
import com.bi.springbootinit.constant.UserConstant;
import com.bi.springbootinit.exception.BusinessException;
import com.bi.springbootinit.exception.ThrowUtils;
import com.bi.springbootinit.manager.AIManager;
import com.bi.springbootinit.model.dto.chart.ChartAddRequest;
import com.bi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.bi.springbootinit.model.dto.chart.ChartUpdateRequest;
import com.bi.springbootinit.model.dto.chart.GenChartRequest;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.model.entity.User;
import com.bi.springbootinit.model.vo.BIResponse;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.service.UserService;
import com.bi.springbootinit.utils.ExcelUtils;
import com.bi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 生成图表接口
 * @author Willow
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AIManager aiManager;

    @Resource
    RedissonClient redissonClient;

    /**
     * 生成图表
     *
     * @param chartAddRequest 生成图表属性
     * @param request 网络请求
     * @return 生成图表id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除图表
     *
     * @param deleteRequest 删除图表id
     * @param request 网络请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新图表
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        // 参数校验

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取生成图表
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 生成图表
     *
     * @param multipartFile 上传的文件
     * @param genChartRequest 生成图表所需信息
     * @param request 前端请求
     * @return 生成图表
     */
    @PostMapping("/gen")
    public BaseResponse<BIResponse> genChart(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartRequest genChartRequest, HttpServletRequest request) {
        String chartType = genChartRequest.getChartType();
        String goal = genChartRequest.getGoal();
        String chartName = genChartRequest.getChartName();
        User loginUser = userService.getLoginUser(request);
        long modelId = 1709156902984093697L;
        // 限流
        redissonClient.getRateLimiter("genChart_" + loginUser.getId());

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
        boolean save = chartService.save(chart);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图表到数据库失败");
        }

        BIResponse response = new BIResponse();
        response.setChartId(chart.getId());
        response.setGenChart(genChart);
        response.setGenResult(genResult);
        return ResultUtils.success(response);
    }



//    /**
//     * 分页获取列表（仅管理员）
//     *
//     * @param chartQueryRequest
//     * @return
//     */
//    @PostMapping("/list/page")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
//        long current = chartQueryRequest.getCurrent();
//        long size = chartQueryRequest.getPageSize();
//        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
//                chartService.getQueryWrapper(chartQueryRequest));
//        return ResultUtils.success(chartPage);
//    }
//
    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }
//
    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

//    // endregion
//
//    /**
//     * 分页搜索（从 ES 查询，封装类）
//     *
//     * @param chartQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/search/page/vo")
//    public BaseResponse<Page<Chart>> searchChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
//            HttpServletRequest request) {
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> chartPage = chartService.searchFromEs(chartQueryRequest);
//        return ResultUtils.success(chartService.getChartPage(chartPage, request));
//    }
//
//    /**
//     * 编辑（用户）
//     *
//     * @param chartEditRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/edit")
//    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
//        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        Chart chart = new Chart();
//        BeanUtils.copyProperties(chartEditRequest, chart);
//        List<String> tags = chartEditRequest.getTags();
//        if (tags != null) {
//            chart.setTags(JSONUtil.toJsonStr(tags));
//        }
//        // 参数校验
//        chartService.validChart(chart, false);
//        User loginUser = userService.getLoginUser(request);
//        long id = chartEditRequest.getId();
//        // 判断是否存在
//        Chart oldChart = chartService.getById(id);
//        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或管理员可编辑
//        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        boolean result = chartService.updateById(chart);
//        return ResultUtils.success(result);
//    }

}

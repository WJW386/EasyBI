package com.bi.springbootinit.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bi.springbootinit.annotation.AuthCheck;
import com.bi.springbootinit.common.BaseResponse;
import com.bi.springbootinit.common.DeleteRequest;
import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.common.ResultUtils;
import com.bi.springbootinit.constant.CommonConstant;
import com.bi.springbootinit.constant.UserConstant;
import com.bi.springbootinit.exception.BusinessException;
import com.bi.springbootinit.exception.ThrowUtils;
import com.bi.springbootinit.model.dto.chart.ChartAddRequest;
import com.bi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.bi.springbootinit.model.dto.chart.ChartUpdateRequest;
import com.bi.springbootinit.model.dto.chart.GenChartRequest;
import com.bi.springbootinit.model.dto.file.UploadFileRequest;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.model.entity.User;
import com.bi.springbootinit.model.enums.FileUploadBizEnum;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.service.UserService;
import com.bi.springbootinit.utils.ExcelUtils;
import com.bi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;

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
    public BaseResponse<String> genChart(@RequestPart("file") MultipartFile multipartFile,
                                         GenChartRequest genChartRequest, HttpServletRequest request) {
        Long id = genChartRequest.getId();
        String chartType = genChartRequest.getChartType();
        String goal = genChartRequest.getGoal();
        String chartName = genChartRequest.getChartName();

        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() >= 100,
                ErrorCode.PARAMS_ERROR, "表名过长");

        String data = ExcelUtils.excelToCsv(multipartFile);

        User loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        File file = null;
        try {
            return ResultUtils.success("");
        } catch (Exception e) {
//            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
//                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest 查询请求
     * @return 查询图表包装类
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
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
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
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
//    /**
//     * 分页获取列表（封装类）
//     *
//     * @param chartQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/list/page/vo")
//    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
//            HttpServletRequest request) {
//        long current = chartQueryRequest.getCurrent();
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
//                chartService.getQueryWrapper(chartQueryRequest));
//        return ResultUtils.success(chartService.getChartPage(chartPage, request));
//    }
//
//    /**
//     * 分页获取当前用户创建的资源列表
//     *
//     * @param chartQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/my/list/page/vo")
//    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
//            HttpServletRequest request) {
//        if (chartQueryRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        chartQueryRequest.setUserId(loginUser.getId());
//        long current = chartQueryRequest.getCurrent();
//        long size = chartQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
//                chartService.getQueryWrapper(chartQueryRequest));
//        return ResultUtils.success(chartService.getChartPage(chartPage, request));
//    }
//
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

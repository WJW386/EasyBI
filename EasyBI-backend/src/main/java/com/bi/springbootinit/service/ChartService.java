package com.bi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.bi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bi.springbootinit.model.entity.User;
import com.bi.springbootinit.model.vo.BIResponse;
import org.springframework.web.multipart.MultipartFile;

/**
* @author Yurio
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2024-03-06 23:27:50
*/
public interface ChartService extends IService<Chart> {

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest 查询请求
     * @return 查询图表包装类
     */
   QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    /**
     * 异步生成图表
     *
     * @param multipartFile 上传文件
     * @param chartType 图表类型
     * @param goal 分析目标
     * @param chartName 图表名称
     * @param loginUser 登录用户
     * @param modelId 使用的AI模型id
     * @return 生成结果
     */
    BIResponse genChartAsync(MultipartFile multipartFile, String chartType, String goal, String chartName, User loginUser, long modelId);

    /**
     * 同步生成图表
     *
     * @param multipartFile 上传文件
     * @param chartType 图表类型
     * @param goal 分析目标
     * @param chartName 图表名称
     * @param loginUser 登录用户
     * @param modelId 使用的AI模型id
     * @return 生成结果
     */
    BIResponse genChart(MultipartFile multipartFile, String chartType, String goal, String chartName, User loginUser, long modelId);

    /**
     * 处理异常方法
     *
     * @param chartId 图表id
     * @param execMessage 处理信息
     * @param input 用户输入
     */
    void handleChartUpdateError(long chartId, String execMessage, String input);
}

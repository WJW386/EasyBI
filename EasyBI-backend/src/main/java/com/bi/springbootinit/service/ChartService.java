package com.bi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.bi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

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
}

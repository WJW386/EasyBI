package com.bi.springbootinit.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 生成图表请求
 *
 * @author Willow
 **/
@Data
public class GenChartRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 生成目标
     */
    private String goal;

    /**
     * 图表种类
     */
    private String chartType;

    /**
     * 图表名称
     */
    private String chartName;

    private static final long serialVersionUID = 1L;
}

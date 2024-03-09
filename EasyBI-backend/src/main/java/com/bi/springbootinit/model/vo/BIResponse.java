package com.bi.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Willow
 **/
@Data
public class BIResponse implements Serializable {

    /**
     * id
     */
    private Long chartId;

    /**
     * 图表
     */
    private String genChart;

    /**
     * 结论
     */
    private String genResult;

    private static final long serialVersionUID = 1L;
}

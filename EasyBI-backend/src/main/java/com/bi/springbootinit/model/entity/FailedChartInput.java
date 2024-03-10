package com.bi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 失败图表输入表
 * @TableName failed_chart_input
 */
@TableName(value ="failed_chart_input")
@Data
public class FailedChartInput implements Serializable {
    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 模型输入信息
     */
    private String input;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
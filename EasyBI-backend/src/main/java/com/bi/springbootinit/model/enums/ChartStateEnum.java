package com.bi.springbootinit.model.enums;

import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.exception.ThrowUtils;
import org.apache.commons.lang3.ObjectUtils;

/**
 * @author Willow
 **/

public enum ChartStateEnum {
    /**
     * 等待执行该任务
     */
    WAIT("等待执行", "wait"),
    /**
     * 正在执行该任务
     */
    RUNNING("正在执行", "running"),
    /**
     * 成功执行完成任务
     */
    SUCCEED("执行成功", "succeed"),
    /**
     * 该任务执行失败
     */
    FAILED("执行失败", "failed");

    private final String text;

    private final String value;

    ChartStateEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ChartStateEnum getEnumByValue(String value) {
        ThrowUtils.throwIf(ObjectUtils.isEmpty(value), ErrorCode.PARAMS_ERROR, "枚举值为空");
        for (ChartStateEnum anEnum : ChartStateEnum.values()) {
            if (value.equals(anEnum.getValue())) {
                return anEnum;
            }
        }
        return null;
    }

    public String getText() {
        return text;
    }

    public String getValue() {
        return value;
    }
}

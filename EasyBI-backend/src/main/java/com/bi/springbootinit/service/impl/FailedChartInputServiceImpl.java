package com.bi.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bi.springbootinit.model.entity.FailedChartInput;
import com.bi.springbootinit.service.FailedChartInputService;
import com.bi.springbootinit.mapper.FailedChartInputMapper;
import org.springframework.stereotype.Service;

/**
* @author Yurio
* @description 针对表【failed_chart_input(失败图表输入表)】的数据库操作Service实现
* @createDate 2024-03-09 17:16:23
*/
@Service
public class FailedChartInputServiceImpl extends ServiceImpl<FailedChartInputMapper, FailedChartInput>
    implements FailedChartInputService{

}





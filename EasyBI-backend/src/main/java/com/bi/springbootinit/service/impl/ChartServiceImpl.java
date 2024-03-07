package com.bi.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bi.springbootinit.model.entity.Chart;
import com.bi.springbootinit.service.ChartService;
import com.bi.springbootinit.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author Yurio
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-03-06 23:27:50
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}





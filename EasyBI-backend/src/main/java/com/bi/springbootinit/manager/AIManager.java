package com.bi.springbootinit.manager;

import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.exception.ThrowUtils;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Willow
 **/
@Service
public class AIManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    public String doChat(long modelId, String message) {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setMessage(message);
        devChatRequest.setModelId(modelId);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        ThrowUtils.throwIf(response == null, ErrorCode.SYSTEM_ERROR, "模型生成回复失败");
        return response.getData().getContent();
    }
}

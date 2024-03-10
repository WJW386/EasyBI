package com.bi.springbootinit.manager;

import com.bi.springbootinit.common.ErrorCode;
import com.bi.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 提供 RedisLimiter 限流基础服务
 *
 * @author Willow
 **/
@Service
public class RedisLimitManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     *  进行限流
     *
     * @param key 区分不同的限流器
     */
    public void doRateLimit(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 每秒最多访问一次
        rateLimiter.setRate(RateType.OVERALL, 1, 1, RateIntervalUnit.SECONDS);
        boolean result = rateLimiter.tryAcquire(1);
        // 未能获得令牌
        if (!result) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}

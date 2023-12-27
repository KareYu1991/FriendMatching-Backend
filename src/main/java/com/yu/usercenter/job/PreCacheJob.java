package com.yu.usercenter.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yu.usercenter.model.domain.User;
import com.yu.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component // 加载成bean
public class PreCacheJob {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Resource
    private RedissonClient redissonClient;

    //重点用户  这里先写一个id为1的用户
    private final List<Long> mainUserList = Arrays.asList(1L);

    //每天执行，预热推荐用户 秒-分-时-日-月-年
    @Scheduled(cron = "0 24 18 * * *")
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("it:user:recommend:docache:lock");
        try {
            // 只有一个线程获取锁
            if (lock.tryLock(0, 30000L, TimeUnit.MILLISECONDS)) {
                log.debug("getLock: " + Thread.currentThread().getId());
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                for (Long userId : mainUserList) {
                    Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper);
                    String redisKey = String.format("partner:user:recommend:%s", userId);
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    //写缓存
                    try {
                        valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 只释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                log.debug("unlock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }
}
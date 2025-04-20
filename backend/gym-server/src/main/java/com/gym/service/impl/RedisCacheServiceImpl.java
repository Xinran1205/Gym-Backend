package com.gym.service.impl;

import com.gym.bloomFilter.BloomFilterUtil;
import com.gym.dao.UserDao;
import com.gym.dto.redis.UserCacheDTO;
import com.gym.entity.User;
import com.gym.service.RedisCacheService;
import com.gym.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 针对用户数据的 Redis 缓存服务
 * 主要实现了：
 * 1. 利用布隆过滤器防止缓存穿透
 * 2. 利用分布式锁和随机 TTL 防止缓存雪崩
 */
@Service
@Slf4j
public class RedisCacheServiceImpl implements RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 直接注入 UserDao（或 UserMapper），而非 UserService
    @Autowired
    private UserDao userDao;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    /**
     * 获取用户信息，同时防止缓存穿透和缓存雪崩
     */
    public User getUser(Long userId) {
        // 针对用户数据使用布隆过滤器检查是否存在
        if (!bloomFilterUtil.mightContainUser(userId)) {
            return null;
        }
        String key = "USER:" + userId;
        Object cacheObj = redisTemplate.opsForValue().get(key);
        if (cacheObj != null) {
            if (cacheObj instanceof NullValue) {
                return null;
            }
            UserCacheDTO dto = (UserCacheDTO) cacheObj;
            return dto.toEntity();
        }
        // 缓存未命中，采用分布式锁避免大量请求并发查询数据库
        String lockKey = "LOCK:USER:" + userId;
        String lockValue = UUID.randomUUID().toString();
        // 这里就是使用了setNx
        boolean locked = tryLock(lockKey, lockValue, 10);
        if (locked) {
            try {
                // 双重检查
                cacheObj = redisTemplate.opsForValue().get(key);
                if (cacheObj != null) {
                    if (cacheObj instanceof NullValue) {
                        return null;
                    }
                    UserCacheDTO dto = (UserCacheDTO) cacheObj;
                    return dto.toEntity();
                }
                // 查询数据库
                User user = userDao.selectById(userId);
                if (user != null) {
                    // 设置随机过期时间，防止缓存大量同时失效
//                    int baseTtl = 600; // 基础10分钟
//                    int randomExtra = new Random().nextInt(60); // 0-59秒随机额外时间
//                    int ttl = baseTtl + randomExtra;
//                    redisTemplate.opsForValue().set(key, UserCacheDTO.fromEntity(user), ttl, TimeUnit.SECONDS);
                    updateUser(user);
                    return user;
                } else {
                    // 数据库中不存在，缓存空值防止缓存穿透（空值缓存有效期较短）
                    redisTemplate.opsForValue().set(key, new NullValue(), 300, TimeUnit.SECONDS);
                    return null;
                }
            } finally {
                unlock(lockKey, lockValue);
            }
        } else {
            // 获取不到锁，则稍后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUser(userId);
        }
    }

    // 更新缓存
    public void updateUser(User user) {
        int baseTtl = 600; // 基础10分钟
        int randomExtra = new Random().nextInt(60); // 0-59秒随机额外时间
        int ttl = baseTtl + randomExtra;
        String key = "USER:" + user.getUserID();
        redisTemplate.opsForValue().set(key, UserCacheDTO.fromEntity(user), ttl, TimeUnit.SECONDS);
    }

    // 删除缓存
    // 比如数据更新了
    public void deleteUser(Long userId) {
        String key = "USER:" + userId;
        redisTemplate.delete(key);
    }

    // 基于 Redis 的简单分布式锁实现
    private boolean tryLock(String lockKey, String lockValue, int expireSeconds) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);
        return success != null && success;
    }

    private void unlock(String lockKey, String lockValue) {
        Object value = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(value)) {
            redisTemplate.delete(lockKey);
        }
    }

    // 用于缓存空值的标记类
    public static class NullValue implements Serializable {}
}

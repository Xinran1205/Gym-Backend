package com.gym.service;


import com.gym.entity.User;

public interface RedisCacheService {
    User getUser(Long userId);

}

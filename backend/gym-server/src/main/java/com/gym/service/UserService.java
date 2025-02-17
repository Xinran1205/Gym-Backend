package com.gym.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.gym.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    User createUser(User user);

    List<User> getAllUsers();

    User getUserById(Long userID);

    // 新增
    User getByEmail(String email);
}

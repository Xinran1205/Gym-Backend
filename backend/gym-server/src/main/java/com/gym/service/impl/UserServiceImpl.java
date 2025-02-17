package com.gym.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.UserDao;
import com.gym.entity.User;
import com.gym.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Override
    public User createUser(User user) {
        baseMapper.insert(user);
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return baseMapper.selectList(null);
    }

    @Override
    public User getUserById(Long userID) {
        return baseMapper.selectById(userID);
    }

    @Override
    public User getByEmail(String email) {
        // 使用 MyBatis-Plus 提供的 LambdaQuery
        return this.lambdaQuery().eq(User::getEmail, email).one();
    }
}


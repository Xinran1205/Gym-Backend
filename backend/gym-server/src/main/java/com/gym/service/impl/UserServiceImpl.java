package com.gym.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gym.dao.UserDao;
import com.gym.entity.User;
import com.gym.enumeration.ErrorCode;
import com.gym.exception.CustomException;
import com.gym.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Override
    public User createUser(User user) {
        int insertCount = baseMapper.insert(user);
        if (insertCount <= 0) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to create user.");
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return baseMapper.selectList(null);
    }

    @Override
    public User getUserById(Long userID) {
        User user = baseMapper.selectById(userID);
//        if (user == null) {
//            throw new CustomException(ErrorCode.NOT_FOUND, "User not found.");
//        }
        return user;
    }

    @Override
    public User getByEmail(String email) {
        User user = this.lambdaQuery()
                .eq(User::getEmail, email)
                .one();
        // 这个地方别加异常，因为用户存在很正常！
//        if (user == null) {
//            throw new CustomException(ErrorCode.NOT_FOUND, "Email not found.");
//        }
        return user;
    }
}

package com.gym.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDao extends BaseMapper<User> {
}

package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationDao extends BaseMapper<Notification> {
}


package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.SocialMediaAuth;
import com.gym.entity.TrainerProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SocialMediaAuthDao extends BaseMapper<SocialMediaAuth> {
}

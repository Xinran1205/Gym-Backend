package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.entity.AdminProfile;
import com.gym.entity.FitnessCentre;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminProfileDao extends BaseMapper<AdminProfile> {
}

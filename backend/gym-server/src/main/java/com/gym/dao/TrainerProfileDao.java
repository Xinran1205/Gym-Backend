package com.gym.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.dto.TrainerProfileQuery;
import com.gym.entity.TrainerProfile;
import com.gym.vo.TrainerProfileVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TrainerProfileDao extends BaseMapper<TrainerProfile> {
    @Select("<script>" +
            "SELECT tp.user_id, tp.name, tp.certifications, tp.specializations, tp.years_of_experience, tp.biography, tp.workplace, " +
            "COALESCE(tcr.status, 'NONE') AS connectStatus " +
            "FROM trainer_profiles tp " +
            "LEFT JOIN trainer_connect_requests tcr ON tp.user_id = tcr.trainer_id AND tcr.member_id = #{memberId} " +
            "<where> " +
            "  <if test='query.specializations != null and query.specializations != \"\"'> " +
            "    AND tp.specializations = #{query.specializations} " +
            "  </if> " +
            "  <if test='query.workplace != null and query.workplace != \"\"'> " +
            "    AND tp.workplace = #{query.workplace} " +
            "  </if> " +
            "</where> " +
            "ORDER BY tp.user_id" +
            "</script>")
    Page<TrainerProfileVO> selectTrainersWithConnectStatus(Page<?> page,
                                                           @Param("query") TrainerProfileQuery query,
                                                           @Param("memberId") Long memberId);
}

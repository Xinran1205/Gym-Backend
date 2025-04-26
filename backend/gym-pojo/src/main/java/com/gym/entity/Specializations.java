package com.gym.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

// 这个是存储教练的专业领域的表
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@TableName("specializations")
public class Specializations {
    @TableId(value = "specialization_id", type = IdType.AUTO)
//    @TableField("specialization_id")
    private Long specializationId;

    @TableField("description")
    private String description;
}

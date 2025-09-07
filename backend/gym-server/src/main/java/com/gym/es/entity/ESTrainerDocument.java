package com.gym.es.entity;

import com.gym.entity.TrainerProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ES教练搜索文档实体类
 * 
 * 功能说明：
 * 1. 映射到Elasticsearch的教练索引
 * 2. 支持全文搜索、地理位置搜索、多维度筛选
 * 3. 优化搜索性能和相关性评分
 * 
 * 为什么设计独立的ES实体：
 * 1. 搜索优化：专门为搜索场景设计字段类型和分析器
 * 2. 性能考虑：只包含搜索相关字段，减少索引大小
 * 3. 灵活性：可以添加搜索专用字段，如评分、标签等
 * 4. 解耦：数据库实体变更不影响搜索功能
 * 
 * @author gym-system
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(
    indexName = "gym_trainers",           // 索引名称
    shards = 3,                          // 分片数量（提高性能）
    replicas = 1,                        // 副本数量（提高可用性）
    refreshInterval = "1s"               // 刷新间隔（近实时搜索）
)
public class ESTrainerDocument {

    /**
     * 文档ID - 对应数据库中的trainer_profile_id
     */
    @Id
    private String id;

    /**
     * 教练用户ID - 关联用户表
     */
    @Field(type = FieldType.Long, name = "user_id")
    private Long userId;

    /**
     * 教练姓名 - 支持分词搜索
     */
    @Field(
        type = FieldType.Text, 
        name = "name",
        analyzer = "standard",         // 索引时使用标准分词
        searchAnalyzer = "standard"    // 搜索时使用标准分词
    )
    private String name;

    /**
     * 专业领域 - 支持多选和精确匹配
     */
    @Field(type = FieldType.Keyword, name = "specializations")
    private List<String> specializations;

    /**
     * 认证资质 - 全文搜索字段
     */
    @Field(type = FieldType.Text, name = "certifications", analyzer = "standard")
    private String certifications;

    /**
     * 工作经验年限 - 支持范围查询
     */
    @Field(type = FieldType.Integer, name = "years_of_experience")
    private Integer yearsOfExperience;

    /**
     * 个人简介 - 全文搜索字段
     */
    @Field(type = FieldType.Text, name = "biography", analyzer = "standard")
    private String biography;

    /**
     * 工作地点 - 支持地理位置搜索
     */
    @GeoPointField
    private ESGeoPoint location;

    /**
     * 工作场所名称 - 关键词搜索
     */
    @Field(type = FieldType.Keyword, name = "workplace")
    private String workplace;

    /**
     * 评分 - 支持范围查询和排序
     */
    @Field(type = FieldType.Float, name = "rating")
    private Float rating;

    /**
     * 评价数量 - 影响排序权重
     */
    @Field(type = FieldType.Integer, name = "review_count")
    private Integer reviewCount;

    /**
     * 价格范围 - 支持价格筛选
     */
    @Field(type = FieldType.Float, name = "price_per_session")
    private Float pricePerSession;

    /**
     * 标签 - 用于个性化推荐和特殊筛选
     */
    @Field(type = FieldType.Keyword, name = "tags")
    private List<String> tags;

    /**
     * 创建时间 - 用于新教练排序
     */
    @Field(type = FieldType.Date, name = "created_at", format = DateFormat.date_time)
    private LocalDateTime createdAt;

    /**
     * 更新时间 - 用于数据同步
     */
    @Field(type = FieldType.Date, name = "updated_at", format = DateFormat.date_time)
    private LocalDateTime updatedAt;

    /**
     * 是否在线 - 实时状态
     */
    @Field(type = FieldType.Boolean, name = "is_online")
    private Boolean isOnline;

    /**
     * 响应速度评分 - 影响排序
     */
    @Field(type = FieldType.Float, name = "response_speed_score")
    private Float responseSpeedScore;

    /**
     * 地理位置内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ESGeoPoint {
        private Double lat;  // 纬度
        private Double lon;  // 经度
    }

    /**
     * 从数据库实体转换为ES文档
     * 
     * @param trainerProfile 数据库中的教练信息
     * @return ES搜索文档
     */
    public static ESTrainerDocument fromTrainerProfile(TrainerProfile trainerProfile) {
        // 处理专业领域（兼容Java 8）
        List<String> specializationsList = Collections.emptyList();
        if (trainerProfile.getSpecializations() != null && !trainerProfile.getSpecializations().isEmpty()) {
            specializationsList = Arrays.asList(trainerProfile.getSpecializations().split(","));
        }

        // 默认标签（兼容Java 8）
        List<String> defaultTags = Arrays.asList("优质教练");

        return ESTrainerDocument.builder()
                .id(trainerProfile.getTrainerProfileId().toString())
                .userId(trainerProfile.getUserId())
                .name(trainerProfile.getName())
                .specializations(specializationsList)
                .certifications(trainerProfile.getCertifications())
                .yearsOfExperience(trainerProfile.getYearsOfExperience())
                .biography(trainerProfile.getBiography())
                .workplace(trainerProfile.getWorkplace())
                .createdAt(trainerProfile.getCreateTime())
                .updatedAt(trainerProfile.getUpdateTime())
                // 默认值设置
                .rating(4.5f)  // 新教练默认评分
                .reviewCount(0)
                .pricePerSession(200.0f)  // 默认价格
                .isOnline(true)
                .responseSpeedScore(4.0f)
                .tags(defaultTags)
                .build();
    }
}


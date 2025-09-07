package com.gym.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.List;

/**
 * 教练搜索请求DTO（简化版）
 * 
 * 功能说明：
 * 1. 封装基础的搜索条件
 * 2. 支持关键词和地理位置搜索
 * 3. 包含基础的分页和排序
 * 
 * 为什么简化：
 * 1. 学习友好：减少参数复杂度，专注核心功能
 * 2. 易于理解：清晰的字段含义，便于掌握
 * 3. 实用性强：覆盖最常见的搜索场景
 * 
 * @author gym-system
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSearchRequest {

    /**
     * 搜索关键词
     * 支持搜索：教练姓名、专业领域
     */
    private String keyword;

    /**
     * 专业领域筛选
     */
    private List<String> specializations;

    /**
     * 最低评分筛选
     */
    @Min(value = 0, message = "评分不能小于0")
    @Max(value = 5, message = "评分不能大于5")
    private Float minRating;

    /**
     * 最低工作经验年限
     */
    @Min(value = 0, message = "工作经验不能小于0年")
    private Integer minExperience;

    /**
     * 地理位置搜索 - 用户当前纬度
     */
    private Double userLatitude;

    /**
     * 地理位置搜索 - 用户当前经度
     */
    private Double userLongitude;

    /**
     * 搜索半径（单位：公里）
     */
    @Min(value = 1, message = "搜索半径不能小于1公里")
    @Max(value = 50, message = "搜索半径不能大于50公里")
    private Double radiusKm = 10.0;  // 默认10公里

    /**
     * 排序方式
     */
    private SortType sortBy = SortType.RELEVANCE;

    /**
     * 排序方向
     */
    private SortDirection sortDirection = SortDirection.DESC;

    /**
     * 分页参数 - 页码（从1开始）
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    /**
     * 分页参数 - 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 20, message = "每页大小不能大于20")
    private Integer size = 10;

    /**
     * 排序类型枚举（简化版）
     */
    public enum SortType {
        RELEVANCE,      // 相关性排序（ES默认评分）
        DISTANCE,       // 距离排序
        RATING,         // 评分排序
        EXPERIENCE      // 经验排序
    }

    /**
     * 排序方向枚举
     */
    public enum SortDirection {
        ASC,    // 升序
        DESC    // 降序
    }
}

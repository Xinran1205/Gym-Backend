package com.gym.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教练搜索响应VO（简化版）
 * 
 * 功能说明：
 * 1. 封装基础的搜索结果数据
 * 2. 包含分页信息
 * 3. 保留核心的教练信息字段
 * 
 * 为什么简化：
 * 1. 学习友好：减少字段复杂度，专注核心概念
 * 2. 易于理解：清晰的响应结构，便于掌握
 * 3. 实用性强：包含搜索所需的基本信息
 * 
 * @author gym-system
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerSearchResponse {

    /**
     * 搜索结果列表
     */
    private List<TrainerSearchItem> trainers;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 教练搜索结果项（简化版）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainerSearchItem {

        /**
         * 教练ID
         */
        private String id;

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 教练姓名
         */
        private String name;

        /**
         * 专业领域列表
         */
        private List<String> specializations;

        /**
         * 工作经验年限
         */
        private Integer yearsOfExperience;

        /**
         * 个人简介
         */
        private String biography;

        /**
         * 地理位置
         */
        private GeoLocation location;

        /**
         * 工作场所
         */
        private String workplace;

        /**
         * 评分
         */
        private Float rating;

        /**
         * 评价数量
         */
        private Integer reviewCount;

        /**
         * 距离用户的距离（公里）
         */
        private Double distanceKm;

        /**
         * 是否在线
         */
        private Boolean isOnline;

        /**
         * 创建时间
         */
        private LocalDateTime createdAt;
    }

    /**
     * 地理位置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {
        private Double latitude;   // 纬度
        private Double longitude;  // 经度
        private String address;    // 详细地址
    }

    /**
     * 分页信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private Integer currentPage;    // 当前页码
        private Integer pageSize;       // 每页大小
        private Long totalElements;     // 总记录数
        private Integer totalPages;     // 总页数
        private Boolean hasNext;        // 是否有下一页
        private Boolean hasPrevious;    // 是否有上一页
    }
}

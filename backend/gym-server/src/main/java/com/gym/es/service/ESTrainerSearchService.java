package com.gym.es.service;

import com.gym.dto.TrainerSearchRequest;
import com.gym.vo.TrainerSearchResponse;

/**
 * ES教练搜索服务接口（简化版）
 * 
 * 功能说明：
 * 1. 提供基础的教练搜索功能
 * 2. 支持关键词搜索
 * 3. 保留数据同步接口用于学习
 * 
 * 为什么简化：
 * 1. 学习友好：专注于核心概念，避免过度复杂
 * 2. 循序渐进：先掌握基础，再学习高级功能
 * 3. 易于理解：减少业务复杂度，突出技术要点
 * 
 * @author gym-system
 * @version 1.0
 */
public interface ESTrainerSearchService {

    /**
     * 搜索教练（简化版）
     * 
     * 支持功能：
     * 1. 关键词搜索：姓名、专业领域
     * 2. 基础筛选：评分、工作经验
     * 
     * @param request 搜索请求参数
     * @return 搜索结果响应
     */
    TrainerSearchResponse searchTrainers(TrainerSearchRequest request);

    /**
     * 根据关键词搜索教练
     * 
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果响应
     */
    TrainerSearchResponse searchByKeyword(String keyword, Integer page, Integer size);

    // ==================== 数据同步功能（委托给专门的同步服务） ====================

    /**
     * 同步单个教练数据到ES
     * 
     * @param trainerId 教练ID
     */
    void syncTrainerToES(Long trainerId);

    /**
     * 批量同步教练数据到ES
     * 
     * @param trainerIds 教练ID列表
     */
    void batchSyncTrainersToES(java.util.List<Long> trainerIds);

    /**
     * 删除ES中的教练数据
     * 
     * @param trainerId 教练ID
     */
    void deleteTrainerFromES(Long trainerId);

    /**
     * 重建教练搜索索引
     * 
     * @return 重建结果
     */
    String rebuildTrainerIndex();
}


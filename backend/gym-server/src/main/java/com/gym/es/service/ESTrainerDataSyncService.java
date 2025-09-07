package com.gym.es.service;

/**
 * ES教练数据同步服务接口（简化版）
 * 
 * 功能说明：
 * 1. 处理MySQL与Elasticsearch之间的数据同步
 * 2. 提供数据一致性检查和修复功能
 * 3. 支持增量同步和全量同步
 * 
 * 为什么简化：
 * 1. 学习友好：专注核心概念，避免过度复杂
 * 2. 易于理解：清晰的同步逻辑，便于掌握
 * 3. 循序渐进：先掌握基础，再学习高级功能
 * 
 * @author gym-system
 * @version 1.0
 */
public interface ESTrainerDataSyncService {

    /**
     * 处理教练创建事件
     * 
     * @param trainerId 教练ID
     */
    void handleTrainerCreated(Long trainerId);

    /**
     * 处理教练信息更新事件
     * 
     * @param trainerId 教练ID
     */
    void handleTrainerUpdated(Long trainerId);

    /**
     * 处理教练删除事件
     * 
     * @param trainerId 教练ID
     */
    void handleTrainerDeleted(Long trainerId);

    /**
     * 检查数据一致性
     * 
     * @return 检查结果
     */
    ESDataConsistencyCheckResult checkDataConsistency();

    /**
     * 修复数据不一致问题
     * 
     * @param checkResult 一致性检查结果
     */
    void repairDataInconsistency(ESDataConsistencyCheckResult checkResult);

    /**
     * 增量数据同步
     * 
     * @param fromTime 起始时间
     * @param toTime 结束时间
     */
    void incrementalSync(java.time.LocalDateTime fromTime, java.time.LocalDateTime toTime);

    /**
     * 数据一致性检查结果
     */
    interface ESDataConsistencyCheckResult {
        boolean isConsistent();
        java.util.List<Long> getMissingInES();
        java.util.List<Long> getExtraInES();
        java.util.List<Long> getInconsistentData();
    }
}


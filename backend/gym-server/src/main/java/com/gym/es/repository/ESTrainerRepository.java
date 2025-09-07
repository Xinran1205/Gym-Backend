package com.gym.es.repository;

import com.gym.es.entity.ESTrainerDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ES教练数据仓库接口
 * 
 * 功能说明：
 * 1. 继承ElasticsearchRepository，获得基础CRUD操作
 * 2. 提供自定义查询方法
 * 3. 支持复杂的搜索条件组合
 * 4. 处理地理位置搜索
 * 
 * 为什么使用ElasticsearchRepository：
 * 1. Spring Data集成：与Spring生态完美结合
 * 2. 自动实现：基础操作自动生成，减少代码量
 * 3. 查询方法：支持方法名查询和@Query注解
 * 4. 分页支持：内置分页和排序功能
 * 
 * @author gym-system
 * @version 1.0
 */
@Repository
public interface ESTrainerRepository extends ElasticsearchRepository<ESTrainerDocument, String> {

    // ==================== 基础查询方法 ====================
    
    /**
     * 根据用户ID查找教练
     * 
     * @param userId 用户ID
     * @return 教练文档
     */
    ESTrainerDocument findByUserId(Long userId);

    /**
     * 根据姓名模糊查询教练
     * 
     * @param name 姓名关键词
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ESTrainerDocument> findByNameContaining(String name, Pageable pageable);

    /**
     * 根据专业领域查询教练
     * 
     * @param specialization 专业领域
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ESTrainerDocument> findBySpecializationsContaining(String specialization, Pageable pageable);

    /**
     * 根据评分范围查询教练
     * 
     * @param minRating 最低评分
     * @param maxRating 最高评分
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ESTrainerDocument> findByRatingBetween(Float minRating, Float maxRating, Pageable pageable);

    /**
     * 根据工作经验范围查询教练
     * 
     * @param minExperience 最低经验年限
     * @param maxExperience 最高经验年限
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ESTrainerDocument> findByYearsOfExperienceBetween(Integer minExperience, Integer maxExperience, Pageable pageable);

    /**
     * 查询在线教练
     * 
     * @param isOnline 是否在线
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ESTrainerDocument> findByIsOnline(Boolean isOnline, Pageable pageable);

    // ==================== 复杂查询方法（使用@Query注解） ====================

    /**
     * 多字段关键词搜索
     * 
     * 搜索策略：
     * 1. 在姓名、专业领域、认证、个人简介中搜索关键词
     * 2. 使用multi_match查询，提高搜索准确性
     * 3. 支持模糊匹配
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^3\", \"specializations^2\", \"certifications\", \"biography\"], \"type\": \"best_fields\", \"fuzziness\": \"AUTO\"}}")
    Page<ESTrainerDocument> searchByKeyword(String keyword, Pageable pageable);

    // ==================== 统计查询方法 ====================

    /**
     * 统计在线教练数量
     * 
     * @return 在线教练数量
     */
    long countByIsOnline(Boolean isOnline);

    /**
     * 统计指定专业领域的教练数量
     * 
     * @param specialization 专业领域
     * @return 教练数量
     */
    long countBySpecializationsContaining(String specialization);

    /**
     * 统计高评分教练数量（评分>=4.0）
     * 
     * @param minRating 最低评分
     * @return 教练数量
     */
    long countByRatingGreaterThanEqual(Float minRating);

    // ==================== 批量操作方法 ====================

    /**
     * 根据用户ID列表查询教练
     * 
     * @param userIds 用户ID列表
     * @return 教练列表
     */
    List<ESTrainerDocument> findByUserIdIn(List<Long> userIds);

    /**
     * 根据ID列表删除教练
     * 
     * @param ids 教练ID列表
     */
    void deleteByIdIn(List<String> ids);

    /**
     * 根据用户ID删除教练
     * 
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}


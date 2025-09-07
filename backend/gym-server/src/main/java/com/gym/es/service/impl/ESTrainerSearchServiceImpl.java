package com.gym.es.service.impl;

import com.gym.dto.TrainerSearchRequest;
import com.gym.es.entity.ESTrainerDocument;
import com.gym.es.repository.ESTrainerRepository;
import com.gym.es.service.ESTrainerSearchService;
import com.gym.vo.TrainerSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ES教练搜索服务实现类（企业级简化版）
 * 
 * 核心功能：
 * 1. 关键词搜索教练（一个完整链路）
 * 2. 企业级异常处理和监控
 * 3. 完善的分页和数据转换
 * 
 * 设计理念：
 * - 简单可靠：专注一个核心搜索功能
 * - 企业级：完善的日志、异常处理、监控
 * - 可扩展：为后续功能预留接口
 * 
 * @author gym-system
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gym.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ESTrainerSearchServiceImpl implements ESTrainerSearchService {

    private final ESTrainerRepository esTrainerRepository;

    // ==================== 核心搜索功能 ====================

    /**
     * 综合搜索教练（简化版）
     * 
     * 企业级特性：
     * 1. 完善的参数校验
     * 2. 详细的业务日志
     * 3. 统一的异常处理
     * 4. 性能监控埋点
     */
    @Override
    public TrainerSearchResponse searchTrainers(TrainerSearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("[ES搜索] 开始搜索教练 - 关键词: {}, 页码: {}, 页大小: {}", 
                    request.getKeyword(), request.getPage(), request.getSize());

            // 1. 参数校验
            validateSearchRequest(request);

            // 2. 构建分页参数
            Pageable pageable = buildPageable(request);

            // 3. 执行搜索
            Page<ESTrainerDocument> searchResult = performSearch(request, pageable);

            // 4. 转换结果
            TrainerSearchResponse response = convertToResponse(searchResult);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[ES搜索] 搜索完成 - 耗时: {}ms, 返回结果: {}, 总数: {}", 
                    duration, searchResult.getNumberOfElements(), searchResult.getTotalElements());

            return response;

        } catch (IllegalArgumentException e) {
            log.warn("[ES搜索] 参数错误 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[ES搜索] 搜索失败 - 耗时: {}ms, 错误: {}", duration, e.getMessage(), e);
            throw new RuntimeException("教练搜索服务暂时不可用，请稍后重试", e);
        }
    }

    /**
     * 关键词搜索（简化版）
     */
    @Override
    public TrainerSearchResponse searchByKeyword(String keyword, Integer page, Integer size) {
        TrainerSearchRequest request = TrainerSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();
        return searchTrainers(request);
    }

    // ==================== 数据同步接口（委托实现） ====================

    @Override
    public void syncTrainerToES(Long trainerId) {
        log.info("[ES同步] 同步教练数据请求 - trainerId: {}", trainerId);
        // 这里委托给专门的数据同步服务
        throw new UnsupportedOperationException("请使用 ESTrainerDataSyncService 进行数据同步");
    }

    @Override
    public void batchSyncTrainersToES(List<Long> trainerIds) {
        log.info("[ES同步] 批量同步教练数据请求 - count: {}", trainerIds.size());
        // 这里委托给专门的数据同步服务
        throw new UnsupportedOperationException("请使用 ESTrainerDataSyncService 进行数据同步");
    }

    @Override
    public void deleteTrainerFromES(Long trainerId) {
        log.info("[ES同步] 删除教练数据请求 - trainerId: {}", trainerId);
        // 这里委托给专门的数据同步服务
        throw new UnsupportedOperationException("请使用 ESTrainerDataSyncService 进行数据同步");
    }

    @Override
    public String rebuildTrainerIndex() {
        log.info("[ES同步] 重建索引请求");
        // 这里委托给专门的数据同步服务
        throw new UnsupportedOperationException("请使用 ESTrainerDataSyncService 进行索引重建");
    }

    // ==================== 私有方法 ====================

    /**
     * 参数校验
     */
    private void validateSearchRequest(TrainerSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("搜索请求不能为空");
        }
        if (request.getPage() == null || request.getPage() < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (request.getSize() == null || request.getSize() < 1 || request.getSize() > 50) {
            throw new IllegalArgumentException("页大小必须在1-50之间");
        }
    }

    /**
     * 构建分页参数
     */
    private Pageable buildPageable(TrainerSearchRequest request) {
        int page = request.getPage() - 1; // 转换为0-based
        int size = request.getSize();
        
        // 默认按相关性排序
        Sort sort = Sort.by("_score").descending();
        
        return PageRequest.of(page, size, sort);
    }

    /**
     * 执行搜索
     */
    private Page<ESTrainerDocument> performSearch(TrainerSearchRequest request, Pageable pageable) {
        String keyword = request.getKeyword();
        
        if (StringUtils.hasText(keyword)) {
            log.debug("[ES搜索] 执行关键词搜索: {}", keyword);
            return esTrainerRepository.searchByKeyword(keyword.trim(), pageable);
        } else {
            log.debug("[ES搜索] 执行默认搜索（获取所有教练）");
            return esTrainerRepository.findAll(pageable);
        }
    }

    /**
     * 转换搜索结果
     */
    private TrainerSearchResponse convertToResponse(Page<ESTrainerDocument> searchResult) {
        // 转换教练列表
        List<TrainerSearchResponse.TrainerSearchItem> trainers = searchResult.getContent()
                .stream()
                .map(this::convertDocumentToItem)
                .collect(Collectors.toList());

        // 构建分页信息
        TrainerSearchResponse.PageInfo pageInfo = TrainerSearchResponse.PageInfo.builder()
                .currentPage(searchResult.getNumber() + 1) // 转换为1-based
                .pageSize(searchResult.getSize())
                .totalElements(searchResult.getTotalElements())
                .totalPages(searchResult.getTotalPages())
                .hasNext(searchResult.hasNext())
                .hasPrevious(searchResult.hasPrevious())
                .build();

        return TrainerSearchResponse.builder()
                .trainers(trainers)
                .pageInfo(pageInfo)
                .build();
    }

    /**
     * 转换ES文档为响应对象
     */
    private TrainerSearchResponse.TrainerSearchItem convertDocumentToItem(ESTrainerDocument doc) {
        // 处理地理位置
        TrainerSearchResponse.GeoLocation location = null;
        if (doc.getLocation() != null) {
            location = TrainerSearchResponse.GeoLocation.builder()
                    .latitude(doc.getLocation().getLat())
                    .longitude(doc.getLocation().getLon())
                    .address(doc.getWorkplace())
                    .build();
        }

        return TrainerSearchResponse.TrainerSearchItem.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .name(doc.getName())
                .specializations(doc.getSpecializations())
                .yearsOfExperience(doc.getYearsOfExperience())
                .biography(doc.getBiography())
                .location(location)
                .workplace(doc.getWorkplace())
                .rating(doc.getRating())
                .reviewCount(doc.getReviewCount())
                .isOnline(doc.getIsOnline())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}


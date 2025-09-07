package com.gym.es.controller;

import com.gym.dto.TrainerSearchRequest;
import com.gym.es.service.ESTrainerDataSyncService;
import com.gym.es.service.ESTrainerSearchService;
import com.gym.result.RestResult;
import com.gym.vo.TrainerSearchResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * ES教练搜索控制器（企业级）
 * 
 * 功能说明：
 * 1. 提供教练搜索API接口
 * 2. 提供数据同步管理接口
 * 3. 企业级的参数校验和异常处理
 * 4. 完善的API文档和日志记录
 * 
 * API设计原则：
 * 1. RESTful风格
 * 2. 统一响应格式
 * 3. 详细的参数说明
 * 4. 完善的错误处理
 * 
 * @author gym-system
 * @version 1.0
 */
@Api(tags = "ES教练搜索接口")
@Slf4j
@RestController
@RequestMapping("/api/es/trainers")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gym.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ESTrainerController {

    private final ESTrainerSearchService esTrainerSearchService;
    private final ESTrainerDataSyncService esTrainerDataSyncService;

    // ==================== 搜索接口 ====================

    /**
     * 综合搜索教练
     * 
     * 这是核心的搜索接口，支持：
     * 1. 关键词搜索
     * 2. 分页查询
     * 3. 结果排序
     */
    @ApiOperation(value = "综合搜索教练", notes = "支持关键词搜索、分页查询")
    @PostMapping("/search")
    public RestResult<TrainerSearchResponse> searchTrainers(
            @ApiParam(value = "搜索请求参数", required = true)
            @Valid @RequestBody TrainerSearchRequest request) {
        
        try {
            log.info("[ES接口] 收到教练搜索请求: {}", request);
            
            TrainerSearchResponse response = esTrainerSearchService.searchTrainers(request);
            
            log.info("[ES接口] 教练搜索成功，返回{}条结果", response.getTrainers().size());
            return RestResult.success(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("[ES接口] 搜索参数错误: {}", e.getMessage());
            return RestResult.error("搜索参数错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("[ES接口] 教练搜索失败", e);
            return RestResult.error("搜索服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 简单关键词搜索（GET接口，便于测试）
     */
    @ApiOperation(value = "关键词搜索教练", notes = "简单的关键词搜索接口，便于测试")
    @GetMapping("/search")
    public RestResult<TrainerSearchResponse> searchByKeyword(
            @ApiParam(value = "搜索关键词", example = "张教练")
            @RequestParam(required = false) String keyword,
            
            @ApiParam(value = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            
            @ApiParam(value = "每页大小，最大20", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            log.info("[ES接口] 关键词搜索: keyword={}, page={}, size={}", keyword, page, size);
            
            // 参数校验
            if (page < 1) {
                return RestResult.error("页码必须大于0");
            }
            if (size < 1 || size > 20) {
                return RestResult.error("每页大小必须在1-20之间");
            }
            
            TrainerSearchResponse response = esTrainerSearchService.searchByKeyword(keyword, page, size);
            
            log.info("[ES接口] 关键词搜索成功，返回{}条结果", response.getTrainers().size());
            return RestResult.success(response);
            
        } catch (Exception e) {
            log.error("[ES接口] 关键词搜索失败", e);
            return RestResult.error("搜索服务暂时不可用，请稍后重试");
        }
    }

    // ==================== 数据同步管理接口 ====================

    /**
     * 手动同步单个教练数据
     */
    @ApiOperation(value = "同步单个教练数据", notes = "手动触发单个教练数据同步到ES")
    @PostMapping("/sync/{trainerId}")
    public RestResult<String> syncTrainer(
            @ApiParam(value = "教练ID", required = true, example = "1")
            @PathVariable Long trainerId) {
        
        try {
            log.info("[ES接口] 收到同步教练请求: trainerId={}", trainerId);
            
            if (trainerId == null || trainerId <= 0) {
                return RestResult.error("教练ID无效");
            }
            
            esTrainerDataSyncService.handleTrainerUpdated(trainerId);
            
            String message = String.format("教练数据同步成功 (ID: %d)", trainerId);
            log.info("[ES接口] {}", message);
            return RestResult.success(message);
            
        } catch (Exception e) {
            log.error("[ES接口] 同步教练数据失败: trainerId={}", trainerId, e);
            return RestResult.error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 检查数据一致性
     */
    @ApiOperation(value = "检查数据一致性", notes = "检查MySQL和ES之间的数据一致性")
    @GetMapping("/consistency/check")
    public RestResult<String> checkDataConsistency() {
        
        try {
            log.info("[ES接口] 收到数据一致性检查请求");
            
            ESTrainerDataSyncService.ESDataConsistencyCheckResult result = 
                    esTrainerDataSyncService.checkDataConsistency();
            
            String message;
            if (result.isConsistent()) {
                message = "数据一致性检查通过，数据状态良好";
            } else {
                message = String.format("发现数据不一致问题：缺失%d条，多余%d条，不一致%d条",
                        result.getMissingInES().size(),
                        result.getExtraInES().size(),
                        result.getInconsistentData().size());
            }
            
            log.info("[ES接口] 数据一致性检查完成: {}", message);
            return RestResult.success(message);
            
        } catch (Exception e) {
            log.error("[ES接口] 数据一致性检查失败", e);
            return RestResult.error("一致性检查失败: " + e.getMessage());
        }
    }

    /**
     * 修复数据不一致
     */
    @ApiOperation(value = "修复数据不一致", notes = "自动修复MySQL和ES之间的数据不一致问题")
    @PostMapping("/consistency/repair")
    public RestResult<String> repairDataInconsistency() {
        
        try {
            log.info("[ES接口] 收到数据不一致修复请求");
            
            // 1. 先检查一致性
            ESTrainerDataSyncService.ESDataConsistencyCheckResult checkResult = 
                    esTrainerDataSyncService.checkDataConsistency();
            
            if (checkResult.isConsistent()) {
                String message = "数据已经一致，无需修复";
                log.info("[ES接口] {}", message);
                return RestResult.success(message);
            }
            
            // 2. 执行修复
            esTrainerDataSyncService.repairDataInconsistency(checkResult);
            
            String message = "数据不一致修复完成";
            log.info("[ES接口] {}", message);
            return RestResult.success(message);
            
        } catch (Exception e) {
            log.error("[ES接口] 数据不一致修复失败", e);
            return RestResult.error("修复失败: " + e.getMessage());
        }
    }

    // ==================== 健康检查接口 ====================

    /**
     * ES服务健康检查
     */
    @ApiOperation(value = "ES服务健康检查", notes = "检查ES服务是否正常运行")
    @GetMapping("/health")
    public RestResult<String> healthCheck() {
        
        try {
            log.debug("[ES接口] 收到健康检查请求");
            
            // 执行一个简单的搜索来检查ES是否正常
            TrainerSearchResponse response = esTrainerSearchService.searchByKeyword("", 1, 1);
            
            String message = String.format("ES服务运行正常，索引中共有教练数据%d条", 
                    response.getPageInfo().getTotalElements());
            
            log.debug("[ES接口] 健康检查通过: {}", message);
            return RestResult.success(message);
            
        } catch (Exception e) {
            log.error("[ES接口] ES服务健康检查失败", e);
            return RestResult.error("ES服务异常: " + e.getMessage());
        }
    }
}


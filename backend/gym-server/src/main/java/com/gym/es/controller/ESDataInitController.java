package com.gym.es.controller;

import com.gym.es.entity.ESTrainerDocument;
import com.gym.es.repository.ESTrainerRepository;
import com.gym.result.RestResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ES演示数据初始化控制器
 * 
 * 功能说明：
 * 1. 初始化ES索引和演示数据
 * 2. 提供数据清理功能
 * 3. 便于学习和测试ES功能
 * 
 * 注意：这是学习用的控制器，生产环境应该移除
 * 
 * @author gym-system
 * @version 1.0
 */
@Api(tags = "ES演示数据管理")
@Slf4j
@RestController
@RequestMapping("/api/es/demo")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gym.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ESDataInitController {

    private final ESTrainerRepository esTrainerRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 初始化ES索引和演示数据
     */
    @ApiOperation(value = "初始化ES演示数据", notes = "创建索引并插入演示数据，便于测试")
    @PostMapping("/init")
    public RestResult<String> initDemoData() {
        try {
            log.info("[ES演示] 开始初始化演示数据");

            // 1. 确保索引存在
            if (!elasticsearchOperations.indexOps(ESTrainerDocument.class).exists()) {
                elasticsearchOperations.indexOps(ESTrainerDocument.class).create();
                elasticsearchOperations.indexOps(ESTrainerDocument.class).putMapping();
                log.info("[ES演示] ES索引创建成功");
            }

            // 2. 创建演示数据
            List<ESTrainerDocument> demoData = createDemoData();

            // 3. 保存到ES
            esTrainerRepository.saveAll(demoData);

            String message = String.format("ES演示数据初始化成功，共创建%d条教练数据", demoData.size());
            log.info("[ES演示] {}", message);
            return RestResult.success(message);

        } catch (Exception e) {
            log.error("[ES演示] 初始化演示数据失败", e);
            return RestResult.error("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 清理演示数据
     */
    @ApiOperation(value = "清理ES演示数据", notes = "删除所有演示数据")
    @DeleteMapping("/clean")
    public RestResult<String> cleanDemoData() {
        try {
            log.info("[ES演示] 开始清理演示数据");

            // 删除所有数据
            esTrainerRepository.deleteAll();

            String message = "ES演示数据清理完成";
            log.info("[ES演示] {}", message);
            return RestResult.success(message);

        } catch (Exception e) {
            log.error("[ES演示] 清理演示数据失败", e);
            return RestResult.error("清理失败: " + e.getMessage());
        }
    }

    /**
     * 重建索引
     */
    @ApiOperation(value = "重建ES索引", notes = "删除并重新创建索引")
    @PostMapping("/rebuild-index")
    public RestResult<String> rebuildIndex() {
        try {
            log.info("[ES演示] 开始重建索引");

            // 1. 删除现有索引
            if (elasticsearchOperations.indexOps(ESTrainerDocument.class).exists()) {
                elasticsearchOperations.indexOps(ESTrainerDocument.class).delete();
                log.info("[ES演示] 旧索引删除成功");
            }

            // 2. 创建新索引
            elasticsearchOperations.indexOps(ESTrainerDocument.class).create();
            elasticsearchOperations.indexOps(ESTrainerDocument.class).putMapping();

            String message = "ES索引重建成功";
            log.info("[ES演示] {}", message);
            return RestResult.success(message);

        } catch (Exception e) {
            log.error("[ES演示] 重建索引失败", e);
            return RestResult.error("重建索引失败: " + e.getMessage());
        }
    }

    /**
     * 获取索引信息
     */
    @ApiOperation(value = "获取ES索引信息", notes = "查看索引状态和数据统计")
    @GetMapping("/info")
    public RestResult<String> getIndexInfo() {
        try {
            log.info("[ES演示] 获取索引信息");

            boolean indexExists = elasticsearchOperations.indexOps(ESTrainerDocument.class).exists();
            long dataCount = esTrainerRepository.count();

            String message = String.format("索引状态: %s, 数据条数: %d", 
                    indexExists ? "存在" : "不存在", dataCount);

            log.info("[ES演示] {}", message);
            return RestResult.success(message);

        } catch (Exception e) {
            log.error("[ES演示] 获取索引信息失败", e);
            return RestResult.error("获取信息失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 创建演示数据
     */
    private List<ESTrainerDocument> createDemoData() {
        List<ESTrainerDocument> demoData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 北京地区的教练
        demoData.add(ESTrainerDocument.builder()
                .id("demo_1")
                .userId(1001L)
                .name("张强教练")
                .specializations(Arrays.asList("力量训练", "增肌减脂"))
                .certifications("国家级健身教练证书、ACSM认证")
                .yearsOfExperience(5)
                .biography("专注力量训练5年，帮助100+学员成功增肌减脂")
                .location(ESTrainerDocument.ESGeoPoint.builder()
                        .lat(39.9042 + (Math.random() - 0.5) * 0.1) // 北京附近随机位置
                        .lon(116.4074 + (Math.random() - 0.5) * 0.1)
                        .build())
                .workplace("北京奥体健身中心")
                .rating(4.8f)
                .reviewCount(156)
                .pricePerSession(300.0f)
                .isOnline(true)
                .responseSpeedScore(4.9f)
                .tags(Arrays.asList("金牌教练", "专业认证"))
                .createdAt(now.minusMonths(6))
                .updatedAt(now)
                .build());

        demoData.add(ESTrainerDocument.builder()
                .id("demo_2")
                .userId(1002L)
                .name("李美女教练")
                .specializations(Arrays.asList("瑜伽", "普拉提", "形体训练"))
                .certifications("RYT200瑜伽认证、普拉提高级认证")
                .yearsOfExperience(3)
                .biography("瑜伽普拉提专家，温柔耐心，擅长形体塑造")
                .location(ESTrainerDocument.ESGeoPoint.builder()
                        .lat(39.9100)
                        .lon(116.4200)
                        .build())
                .workplace("北京瑜伽生活馆")
                .rating(4.9f)
                .reviewCount(89)
                .pricePerSession(250.0f)
                .isOnline(true)
                .responseSpeedScore(4.8f)
                .tags(Arrays.asList("女性教练", "瑜伽专家"))
                .createdAt(now.minusMonths(4))
                .updatedAt(now)
                .build());

        demoData.add(ESTrainerDocument.builder()
                .id("demo_3")
                .userId(1003L)
                .name("王健教练")
                .specializations(Arrays.asList("有氧运动", "减脂塑形", "功能训练"))
                .certifications("NASM-CPT认证、功能训练专家")
                .yearsOfExperience(7)
                .biography("7年健身经验，擅长有氧减脂和功能训练")
                .location(ESTrainerDocument.ESGeoPoint.builder()
                        .lat(39.8950)
                        .lon(116.3950)
                        .build())
                .workplace("北京全民健身中心")
                .rating(4.7f)
                .reviewCount(203)
                .pricePerSession(280.0f)
                .isOnline(false)
                .responseSpeedScore(4.5f)
                .tags(Arrays.asList("资深教练", "减脂专家"))
                .createdAt(now.minusMonths(12))
                .updatedAt(now.minusDays(1))
                .build());

        demoData.add(ESTrainerDocument.builder()
                .id("demo_4")
                .userId(1004L)
                .name("陈小明教练")
                .specializations(Arrays.asList("游泳", "水中健身"))
                .certifications("游泳救生员证、水中健身教练证")
                .yearsOfExperience(4)
                .biography("专业游泳教练，水中健身专家")
                .location(ESTrainerDocument.ESGeoPoint.builder()
                        .lat(39.9200)
                        .lon(116.4300)
                        .build())
                .workplace("北京水立方游泳馆")
                .rating(4.6f)
                .reviewCount(67)
                .pricePerSession(320.0f)
                .isOnline(true)
                .responseSpeedScore(4.3f)
                .tags(Arrays.asList("游泳专家", "水中训练"))
                .createdAt(now.minusMonths(8))
                .updatedAt(now)
                .build());

        demoData.add(ESTrainerDocument.builder()
                .id("demo_5")
                .userId(1005L)
                .name("赵大力教练")
                .specializations(Arrays.asList("举重", "力量训练", "竞技体能"))
                .certifications("国家举重二级运动员、力量举教练")
                .yearsOfExperience(10)
                .biography("前专业举重运动员，10年执教经验")
                .location(ESTrainerDocument.ESGeoPoint.builder()
                        .lat(39.8800)
                        .lon(116.3800)
                        .build())
                .workplace("北京力量训练基地")
                .rating(4.9f)
                .reviewCount(145)
                .pricePerSession(400.0f)
                .isOnline(true)
                .responseSpeedScore(4.7f)
                .tags(Arrays.asList("专业运动员", "力量专家"))
                .createdAt(now.minusMonths(18))
                .updatedAt(now)
                .build());

        log.info("[ES演示] 创建了{}条演示数据", demoData.size());
        return demoData;
    }
}


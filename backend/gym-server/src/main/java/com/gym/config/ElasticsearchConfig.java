package com.gym.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch配置类
 * 
 * 功能说明：
 * 1. 配置ES客户端连接
 * 2. 启用ES仓库功能
 * 3. 提供ES操作模板
 * 4. 支持集群配置
 * 
 * 为什么使用ES：
 * 1. 全文搜索能力强：支持分词、模糊匹配、同义词搜索
 * 2. 复杂查询支持：多条件组合、地理位置搜索、聚合统计
 * 3. 性能优异：倒排索引结构，搜索速度快
 * 4. 实时性好：近实时搜索，数据更新后快速可搜索
 * 5. 可扩展：分布式架构，支持水平扩展
 * 
 * @author gym-system
 * @version 1.0
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.gym.es.repository")
public class ElasticsearchConfig {

    /** ES服务器主机地址 */
    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;

    /** ES服务器端口 */
    @Value("${elasticsearch.port:9200}")
    private int elasticsearchPort;

    /** ES连接协议 */
    @Value("${elasticsearch.scheme:http}")
    private String elasticsearchScheme;

    /** 连接超时时间(毫秒) */
    @Value("${elasticsearch.connection-timeout:5000}")
    private int connectionTimeout;

    /** 读取超时时间(毫秒) */
    @Value("${elasticsearch.socket-timeout:60000}")
    private int socketTimeout;

    /**
     * 配置Elasticsearch REST客户端
     * 
     * 为什么使用RestHighLevelClient：
     * 1. 线程安全：可以在多线程环境中安全使用
     * 2. 连接池：内置连接池管理，提高性能
     * 3. 异步支持：支持同步和异步操作
     * 4. 全功能：支持ES的所有API操作
     * 
     * @return RestHighLevelClient ES高级客户端
     */
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        return new RestHighLevelClient(
            RestClient.builder(
                new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchScheme)
            )
            // 设置连接超时时间
            .setRequestConfigCallback(requestConfigBuilder -> 
                requestConfigBuilder
                    .setConnectTimeout(connectionTimeout)
                    .setSocketTimeout(socketTimeout)
            )
            // 设置HTTP客户端配置
            .setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder
                    .setMaxConnTotal(100)        // 最大连接数
                    .setMaxConnPerRoute(100)     // 每个路由的最大连接数
            )
        );
    }

    /**
     * 配置Elasticsearch操作模板
     * 
     * 为什么需要ElasticsearchRestTemplate：
     * 1. 简化操作：提供高级API，简化ES操作
     * 2. 自动映射：自动处理Java对象与ES文档的转换
     * 3. 异常处理：统一的异常处理机制
     * 4. 事务支持：与Spring事务管理集成
     * 
     * @return ElasticsearchOperations ES操作接口
     */
    @Bean
    public ElasticsearchOperations elasticsearchOperations() {
        return new ElasticsearchRestTemplate(elasticsearchClient());
    }

    /**
     * 配置Elasticsearch模板（别名）
     * 
     * 为了兼容性，提供elasticsearchTemplate别名
     * 
     * @return ElasticsearchOperations ES操作接口
     */
    @Bean(name = "elasticsearchTemplate")
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(elasticsearchClient());
    }
}
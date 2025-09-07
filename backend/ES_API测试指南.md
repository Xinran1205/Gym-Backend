# ES教练搜索系统 - API测试指南

## 🎯 系统概述

本系统实现了一个**企业级的ES教练搜索功能**，包含：
- ✅ **简单但完整的搜索链路**：关键词搜索教练
- ✅ **完善的数据同步机制**：MySQL与ES数据一致性保证
- ✅ **企业级特性**：异常处理、日志记录、监控埋点
- ✅ **演示数据**：开箱即用的测试数据

## 🚀 快速开始

### 1. 启动Elasticsearch
```bash
# 使用Docker启动ES（推荐）
docker run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:7.17.9
```

### 2. 启动应用
```bash
cd backend
mvn spring-boot:run
```

### 3. 初始化演示数据
```http
POST http://localhost:8080/api/es/demo/init
```

## 📋 API接口详情

### 🔧 演示数据管理

#### 初始化演示数据
```http
POST /api/es/demo/init
Content-Type: application/json

响应示例：
{
  "code": 200,
  "message": "操作成功",
  "data": "ES演示数据初始化成功，共创建5条教练数据"
}
```

#### 获取索引信息
```http
GET /api/es/demo/info

响应示例：
{
  "code": 200,
  "message": "操作成功", 
  "data": "索引状态: 存在, 数据条数: 5"
}
```

#### 清理演示数据
```http
DELETE /api/es/demo/clean
```

### 🔍 教练搜索功能

#### 1. 简单关键词搜索（GET接口）
```http
GET /api/es/trainers/search?keyword=张教练&page=1&size=10

响应示例：
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "trainers": [
      {
        "id": "demo_1",
        "userId": 1001,
        "name": "张强教练",
        "specializations": ["力量训练", "增肌减脂"],
        "yearsOfExperience": 5,
        "biography": "专注力量训练5年，帮助100+学员成功增肌减脂",
        "workplace": "北京奥体健身中心",
        "rating": 4.8,
        "reviewCount": 156,
        "isOnline": true,
        "location": {
          "latitude": 39.9042,
          "longitude": 116.4074,
          "address": "北京奥体健身中心"
        }
      }
    ],
    "pageInfo": {
      "currentPage": 1,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1,
      "hasNext": false,
      "hasPrevious": false
    }
  }
}
```

#### 2. 综合搜索（POST接口）
```http
POST /api/es/trainers/search
Content-Type: application/json

{
  "keyword": "瑜伽",
  "page": 1,
  "size": 10,
  "sortBy": "RATING",
  "sortDirection": "DESC"
}

响应格式同上
```

#### 3. 搜索测试用例

**测试用例1：搜索"力量"**
```http
GET /api/es/trainers/search?keyword=力量&page=1&size=5
```

**测试用例2：搜索"瑜伽"**
```http
GET /api/es/trainers/search?keyword=瑜伽&page=1&size=5
```

**测试用例3：搜索"教练"**
```http
GET /api/es/trainers/search?keyword=教练&page=1&size=5
```

**测试用例4：获取所有教练**
```http
GET /api/es/trainers/search?page=1&size=10
```

### ⚙️ 数据同步管理

#### 检查数据一致性
```http
GET /api/es/trainers/consistency/check

响应示例：
{
  "code": 200,
  "message": "操作成功",
  "data": "数据一致性检查通过，数据状态良好"
}
```

#### 修复数据不一致
```http
POST /api/es/trainers/consistency/repair

响应示例：
{
  "code": 200,
  "message": "操作成功",
  "data": "数据不一致修复完成"
}
```

#### 同步单个教练数据
```http
POST /api/es/trainers/sync/1001

响应示例：
{
  "code": 200,
  "message": "操作成功",
  "data": "教练数据同步成功 (ID: 1001)"
}
```

### 🏥 健康检查

#### ES服务健康检查
```http
GET /api/es/trainers/health

响应示例：
{
  "code": 200,
  "message": "操作成功",
  "data": "ES服务运行正常，索引中共有教练数据5条"
}
```

## 🧪 完整测试流程

### Step 1: 环境检查
```bash
# 检查ES是否启动
curl http://localhost:9200

# 检查应用是否启动
curl http://localhost:8080/api/es/trainers/health
```

### Step 2: 初始化数据
```bash
curl -X POST http://localhost:8080/api/es/demo/init
```

### Step 3: 测试搜索功能
```bash
# 测试关键词搜索
curl "http://localhost:8080/api/es/trainers/search?keyword=张教练"

# 测试专业搜索
curl "http://localhost:8080/api/es/trainers/search?keyword=瑜伽"

# 测试分页
curl "http://localhost:8080/api/es/trainers/search?page=1&size=2"
```

### Step 4: 测试数据同步
```bash
# 检查数据一致性
curl http://localhost:8080/api/es/trainers/consistency/check

# 同步数据
curl -X POST http://localhost:8080/api/es/trainers/sync/1001
```

## 📊 演示数据说明

系统预置了5个教练的演示数据：

1. **张强教练** - 力量训练专家（北京奥体健身中心）
2. **李美女教练** - 瑜伽普拉提专家（北京瑜伽生活馆）  
3. **王健教练** - 有氧减脂专家（北京全民健身中心）
4. **陈小明教练** - 游泳专家（北京水立方游泳馆）
5. **赵大力教练** - 举重专家（北京力量训练基地）

每个教练都包含：
- 基本信息（姓名、经验年限）
- 专业领域
- 地理位置（北京地区）
- 评分和评价数
- 在线状态

## 🔍 搜索功能特点

### 1. 多字段搜索
- 支持按教练姓名搜索
- 支持按专业领域搜索
- 支持按认证信息搜索
- 支持按个人简介搜索

### 2. 智能排序
- 相关性排序（默认）
- 评分排序
- 经验排序

### 3. 分页支持
- 灵活的分页参数
- 完整的分页信息返回

## ⚡ 企业级特性

### 1. 异常处理
- 统一的异常处理机制
- 友好的错误信息返回
- 详细的错误日志记录

### 2. 数据一致性
- 自动数据同步机制
- 定时一致性检查
- 手动修复功能

### 3. 监控和日志
- 详细的业务日志
- 性能监控埋点
- 操作审计记录

### 4. 配置管理
- 灵活的ES配置
- 开关控制
- 环境适配

## 🚨 注意事项

1. **ES版本兼容性**：使用ES 7.17.9版本
2. **内存配置**：建议ES分配至少512MB内存
3. **网络连接**：确保应用能访问ES的9200端口
4. **数据持久化**：演示数据存储在ES中，重启ES会丢失
5. **生产环境**：演示接口应在生产环境中移除

## 📚 学习要点

通过这个系统，您可以学习到：

1. **ES基础概念**：索引、文档、查询
2. **Spring Data ES**：Repository、ElasticsearchOperations
3. **数据同步**：MySQL与ES的数据一致性
4. **企业级开发**：异常处理、日志记录、配置管理
5. **API设计**：RESTful接口、统一响应格式

这是一个完整的企业级ES搜索系统的简化版本，专注于核心功能的实现和学习！


# ==========================
# 第一阶段：Build Stage
# ==========================
FROM maven:3.8.6-openjdk-8-slim AS builder
WORKDIR /app

# 1. 拷贝父级pom.xml
COPY ./pom.xml ./

# 2. 拷贝各子模块的pom.xml（便于go-offline缓存依赖）
COPY gym-common/pom.xml  ./gym-common/
COPY gym-pojo/pom.xml    ./gym-pojo/
COPY gym-server/pom.xml  ./gym-server/

# 3. 先下载依赖
RUN mvn dependency:go-offline

# 4. 再拷贝全部源码
COPY . .

# 5. 编译打包
RUN mvn clean package -DskipTests

# ==========================
# 第二阶段：Runtime Stage
# ==========================
FROM openjdk:8-jre-slim
WORKDIR /app

# 关键：一定要写对Jar文件的实际名字
COPY --from=builder /app/gym-server/target/gym-server-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
CMD ["java","-jar","/app/app.jar"]






## ==========================
## 第一阶段：Build Stage
## ==========================
#FROM maven:3.8.6-openjdk-8-slim AS builder
#
#WORKDIR /app
#
## 拷贝 pom.xml 并提前下载依赖（加速后面打包）
#COPY pom.xml /app
#RUN mvn dependency:go-offline
#
## 拷贝源码
#COPY src /app/src
#
## 使用 Maven 打包，跳过测试
#RUN mvn clean package -DskipTests
#
## ==========================
## 第二阶段：Runtime Stage
## ==========================
#FROM openjdk:8-jre-slim
#
#WORKDIR /app
#
## 从第一阶段复制打包生成的 JAR 文件到当前镜像
#COPY --from=builder /app/target/Backend-0.0.1-SNAPSHOT.jar app.jar
#
## Spring Boot 默认为 8080 端口
#EXPOSE 8080
#
## 运行 Spring Boot
#CMD ["java", "-jar", "app.jar"]

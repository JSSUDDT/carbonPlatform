# 碳权平台API

## 项目介绍
这是一个基于Spring Boot, JPA, MySQL的碳权交易平台后端API系统。

## 技术栈
- Spring Boot 2.7.16
- Spring Data JPA
- Spring Security
- JWT认证
- MySQL数据库
- Maven构建

## 项目结构
- `entity`: 实体类，使用carbon前缀
- `repository`: 数据访问层
- `service`: 业务逻辑层
- `controller`: 控制器层
- `dto`: 数据传输对象
- `security`: 安全相关配置
- `config`: 配置类

## API列表
1. API-101 取得Token: `/api/vendor/GetToken`
2. API-102 充电站场域信息: `/api/vendor/StationInfo`
3. API-103 充电站场域充电槍信息: `/api/vendor/ChargerInfo`
4. API-104 场域负载信息传送: `/api/vendor/StationPower`
5. API-105 交易信息上传: `/api/vendor/ChargeInfo`

## 运行说明
1. 确保已安装Java 11或更高版本及Maven
2. 配置application.properties中的数据库连接信息
3. 执行以下命令构建和运行项目:
   ```bash
   mvn clean package
   java -jar target/carbon-0.0.1-SNAPSHOT.jar
   ```

## 安全说明
- 所有API（除了获取Token外）都需要在请求头中加入Bearer Token进行认证
- Token有效期为12小时 
# Carbon API系統

## 数据库更新说明

如果在运行系统时遇到以下错误：
```
Duplicate entry 'CHARGER001' for key 'carbon_charger.PRIMARY'
```

这是因为实体模型已更新，但数据库表结构尚未更新。请执行以下步骤：

1. 备份数据库
2. 执行`src/main/resources/update_charger_table.sql`脚本更新表结构

## 系统更新说明

1. 充电桩和充电枪头数据现已合并到同一个表(carbon_charger)中
2. 充电桩和充电站API现支持统一的updateStatus字段:
   - U = 新增或更新
   - D = 删除 
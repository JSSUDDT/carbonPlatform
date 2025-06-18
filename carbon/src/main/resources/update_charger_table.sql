-- 修改carbon_charger表，将原主键改为复合主键
-- 注意：执行此SQL脚本前请先备份数据库

-- 1. 先删除现有的主键
ALTER TABLE carbon_charger DROP PRIMARY KEY;

-- 2. 添加复合主键(charger_id, connect_id)
ALTER TABLE carbon_charger ADD PRIMARY KEY (charger_id, connect_id);

-- 3. 确保connect_id列不为空
ALTER TABLE carbon_charger MODIFY connect_id VARCHAR(50) NOT NULL;

-- 4. 新增首次啟用日期欄位
ALTER TABLE carbon_charger ADD COLUMN first_enable_date VARCHAR(10) DEFAULT NULL COMMENT '首次啟用日期(YYYY-MM-DD)'; 
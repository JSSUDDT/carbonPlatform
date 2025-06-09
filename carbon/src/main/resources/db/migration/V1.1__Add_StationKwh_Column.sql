-- 為carbon_station_power表添加station_kwh列
ALTER TABLE carbon_station_power ADD COLUMN station_kwh VARCHAR(50) AFTER station_id; 
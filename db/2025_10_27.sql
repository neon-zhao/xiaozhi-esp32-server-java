-- 为角色表添加语音合成高级参数字段
ALTER TABLE `xiaozhi`.`sys_role` 
ADD COLUMN `ttsPitch` FLOAT DEFAULT 1.0 COMMENT '语音音调(0.5-2.0, 默认1.0)' AFTER `voiceName`,
ADD COLUMN `ttsSpeed` FLOAT DEFAULT 1.0 COMMENT '语音语速(0.5-2.0, 默认1.0)' AFTER `ttsPitch`;


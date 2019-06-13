/*
Navicat MySQL Data Transfer

Source Server         : reins@127.0.0.1_本机
Source Server Version : 50719
Source Host           : localhost:3306
Source Database       : reinsdb

Target Server Type    : MYSQL
Target Server Version : 50719
File Encoding         : 65001

Date: 2019-04-22 10:09:31
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for ri_bao
-- ----------------------------
DROP TABLE IF EXISTS `ri_bao`;
CREATE TABLE `ri_bao` (
  `id` int(14) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) DEFAULT NULL,
  `work_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `task_type` varchar(50) DEFAULT NULL,
  `task_no` varchar(300) DEFAULT NULL,
  `work_content` varchar(300) DEFAULT NULL,
  `work_hour` decimal(10,2) DEFAULT NULL,
  `real_hour` decimal(10,2) DEFAULT NULL,
  `project_name` varchar(20) DEFAULT NULL,
  `remark` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=102678 DEFAULT CHARSET=utf8;

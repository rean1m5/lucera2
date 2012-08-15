DROP TABLE IF EXISTS `grandboss_intervallist`;
CREATE TABLE `grandboss_intervallist` (
  `bossId` int(11) NOT NULL,
  `respawnDate` decimal(20,0) NOT NULL,
  `state` int(11) NOT NULL,
  PRIMARY KEY (`bossId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


INSERT INTO `grandboss_intervallist` VALUES ('29019', '0', '0');
INSERT INTO `grandboss_intervallist` VALUES ('29020', '0', '0');
INSERT INTO `grandboss_intervallist` VALUES ('29022', '0', '0');
INSERT INTO `grandboss_intervallist` VALUES ('29028', '0', '0');
INSERT INTO `grandboss_intervallist` VALUES ('29045', '0', '0');
INSERT INTO `grandboss_intervallist` VALUES ('29062', '0', '0');
INSERT INTO `grandboss_intervallist` VALUES ('29065', '0', '0');
INSERT INTO `grandboss_intervallist` VALUES ('29099', '0', '0');
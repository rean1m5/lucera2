DROP TABLE IF EXISTS `seven_signs_festival`;
CREATE TABLE `seven_signs_festival` (
  `festivalId` int(1) NOT NULL DEFAULT '0',
  `cabal` varchar(4) NOT NULL DEFAULT '',
  `cycle` int(4) NOT NULL DEFAULT '0',
  `date` bigint(50) DEFAULT '0',
  `score` int(5) NOT NULL DEFAULT '0',
  `members` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`festivalId`,`cabal`,`cycle`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


INSERT INTO `seven_signs_festival` VALUES ('0', 'dawn', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('1', 'dawn', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('2', 'dawn', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('3', 'dawn', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('4', 'dawn', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('0', 'dusk', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('1', 'dusk', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('2', 'dusk', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('3', 'dusk', '1', '0', '0', '');
INSERT INTO `seven_signs_festival` VALUES ('4', 'dusk', '1', '0', '0', '');
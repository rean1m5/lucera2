DROP TABLE IF EXISTS `castle`;
CREATE TABLE `castle` (
  `id` int(11) NOT NULL DEFAULT '0',
  `name` varchar(25) NOT NULL,
  `taxPercent` int(11) NOT NULL DEFAULT '15',
  `newTaxPercent` int(11) NOT NULL DEFAULT '15',
  `newTaxDate` decimal(20,0) NOT NULL DEFAULT '0',
  `treasury` int(11) NOT NULL DEFAULT '0',
  `bloodaliance` int(11) NOT NULL DEFAULT '0',
  `siegeDate` decimal(20,0) NOT NULL DEFAULT '0',
  `regTimeOver` enum('true','false') NOT NULL DEFAULT 'true',
  `regTimeEnd` decimal(20,0) NOT NULL DEFAULT '0',
  `AutoTime` enum('true','false') NOT NULL DEFAULT 'false',
  PRIMARY KEY (`name`),
  KEY `id` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


INSERT INTO `castle` VALUES ('1', 'Gludio', '0', '0', '0', '0', '0', '1279382400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('2', 'Dion', '0', '0', '0', '0', '0', '1279382400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('3', 'Giran', '0', '0', '0', '0', '0', '1279454400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('4', 'Oren', '0', '0', '0', '0', '0', '1279454400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('5', 'Aden', '0', '0', '0', '0', '0', '1279382400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('6', 'Innadril', '0', '0', '0', '0', '0', '1279454400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('7', 'Goddard', '0', '0', '0', '0', '0', '1279454400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('8', 'Rune', '0', '0', '0', '0', '0', '1279382400000', 'true', '0', 'true');
INSERT INTO `castle` VALUES ('9', 'Schuttgart', '0', '0', '0', '0', '0', '1279454400000', 'true', '0', 'true');
DROP TABLE IF EXISTS `character_quests`;
CREATE TABLE `character_quests` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL DEFAULT '',
  `var` varchar(20) NOT NULL DEFAULT '',
  `value` varchar(255) DEFAULT NULL,
  `class_index` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`,`name`,`var`,`class_index`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
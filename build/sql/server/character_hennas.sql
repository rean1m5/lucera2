DROP TABLE IF EXISTS `character_hennas`;
CREATE TABLE `character_hennas` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `symbol_id` int(11) DEFAULT NULL,
  `slot` int(11) NOT NULL DEFAULT '0',
  `class_index` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`,`slot`,`class_index`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
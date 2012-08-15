DROP TABLE IF EXISTS `character_recommends`;
CREATE TABLE `character_recommends` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `target_id` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`,`target_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
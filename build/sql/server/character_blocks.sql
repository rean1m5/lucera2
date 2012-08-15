DROP TABLE IF EXISTS `character_blocks`;
CREATE TABLE `character_blocks` (
  `charId` int(10) unsigned NOT NULL,
  `name` varchar(35) NOT NULL,
  PRIMARY KEY (`charId`,`name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
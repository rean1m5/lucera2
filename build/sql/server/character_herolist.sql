DROP TABLE IF EXISTS `character_herolist`;
CREATE TABLE `character_herolist` (
  `charId` int(11) NOT NULL DEFAULT '0',
  `enddate` decimal(20,0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
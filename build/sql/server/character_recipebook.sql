DROP TABLE IF EXISTS `character_recipebook`;
CREATE TABLE `character_recipebook` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `id` decimal(11,0) NOT NULL DEFAULT '0',
  `type` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`,`charId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
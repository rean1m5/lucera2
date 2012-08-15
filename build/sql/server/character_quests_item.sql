DROP TABLE IF EXISTS `character_quests_item`;
CREATE TABLE `character_quests_item` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `questId` int(10) NOT NULL,
  `itemId` decimal(11,0) NOT NULL,
  PRIMARY KEY (`charId`,`questId`,`itemId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
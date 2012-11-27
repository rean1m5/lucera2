DROP TABLE IF EXISTS `character_timed_items`;
CREATE TABLE `character_timed_items` (
  `charId` int(11) NOT NULL,
  `itemId` int(11) NOT NULL,
  `time` decimal(20,0) NOT NULL DEFAULT '0'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
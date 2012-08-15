DROP TABLE IF EXISTS `character_offline_shop`;
CREATE TABLE `character_offline_shop` (
  `shopid` int(11) NOT NULL,
  `itemid` int(11) NOT NULL,
  `count` int(11) DEFAULT NULL,
  `price` int(11) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
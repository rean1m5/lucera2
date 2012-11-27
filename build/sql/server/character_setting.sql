DROP TABLE IF EXISTS `character_setting`;
CREATE TABLE `character_setting` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `show_traders` int(10) NOT NULL DEFAULT '1',
  `enable_autoloot` int(11) NOT NULL DEFAULT '0',
  `buff_animation` int(11) NOT NULL DEFAULT '1',
  PRIMARY KEY (`charId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
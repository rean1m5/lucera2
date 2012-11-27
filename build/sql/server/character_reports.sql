DROP TABLE IF EXISTS `character_reports`;
CREATE TABLE `character_reports` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `char_name` varchar(50) NOT NULL DEFAULT '',
  `bot_name` varchar(50) NOT NULL DEFAULT '',
  `date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
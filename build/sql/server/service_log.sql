DROP TABLE IF EXISTS `service_log`;
CREATE TABLE `service_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `serviceName` varchar(50) NOT NULL DEFAULT '',
  `charName` varchar(50) NOT NULL DEFAULT '',
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `message` varchar(50) NOT NULL DEFAULT '',
  `date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
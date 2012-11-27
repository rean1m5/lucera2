DROP TABLE IF EXISTS `gm_audit`;
CREATE TABLE `gm_audit` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `gm_name` varchar(45) DEFAULT NULL,
  `target` varchar(255) DEFAULT NULL,
  `type` varchar(20) DEFAULT NULL,
  `action` varchar(255) DEFAULT NULL,
  `param` varchar(255) DEFAULT NULL,
  `date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
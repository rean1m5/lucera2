DROP TABLE IF EXISTS `fort_functions`;
CREATE TABLE `fort_functions` (
  `fortId` int(2) NOT NULL DEFAULT '0',
  `type` int(1) NOT NULL DEFAULT '0',
  `lvl` int(3) NOT NULL DEFAULT '0',
  `lease` int(10) NOT NULL DEFAULT '0',
  `rate` decimal(20,0) NOT NULL DEFAULT '0',
  `endTime` decimal(20,0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`fortId`,`type`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
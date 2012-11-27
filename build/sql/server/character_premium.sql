DROP TABLE IF EXISTS `character_premium`;
CREATE TABLE `character_premium` (
  `account_name` varchar(45) NOT NULL DEFAULT '',
  `premium_service` int(1) NOT NULL DEFAULT '0',
  `enddate` decimal(20,0) NOT NULL DEFAULT '0',
  `im_cash` int(10) NOT NULL DEFAULT '0',
  PRIMARY KEY (`account_name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
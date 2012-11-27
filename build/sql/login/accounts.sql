DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `login` varchar(45) NOT NULL DEFAULT '',
  `password` varchar(45) DEFAULT NULL,
  `lastactive` decimal(20,0) DEFAULT NULL,
  `accessLevel` int(11) NOT NULL DEFAULT '0',
  `lastIP` varchar(20) DEFAULT NULL,
  `lastServerId` int(11) NOT NULL DEFAULT '1',
  `allowed_ip` varchar(20) NOT NULL DEFAULT '*',
  `allowed_hwid` varchar(250) NOT NULL DEFAULT '*',
  PRIMARY KEY (`login`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
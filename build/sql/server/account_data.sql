DROP TABLE IF EXISTS `account_data`;
CREATE TABLE `account_data` (
  `account_name` varchar(32) NOT NULL,
  `valueName` varchar(32) NOT NULL,
  `valueData` varchar(250) default NULL,
  PRIMARY KEY  (`account_name`,`valueName`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
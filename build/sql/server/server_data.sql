DROP TABLE IF EXISTS `server_data`;
CREATE TABLE `server_data` (
  `valueName` varchar(64) NOT NULL,
  `valueData` varchar(200) NOT NULL,
  PRIMARY KEY (`valueName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
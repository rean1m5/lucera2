-- ----------------------------
-- Table structure for accounts
-- ----------------------------
CREATE TABLE IF NOT EXISTS `accounts` (
  `login` varchar(45) NOT NULL DEFAULT '',
  `password` varchar(45),
  `lastactive` decimal(20,0),
  `accessLevel` INT NOT NULL DEFAULT '0', 
  `lastIP` varchar(20),
  `lastServerId` INT NOT NULL DEFAULT '1',
  `allowed_ip` varchar(20) NOT NULL DEFAULT '*',
  `allowed_hwid` varchar(250) NOT NULL DEFAULT '*',
  PRIMARY KEY (`login`)
) DEFAULT CHARSET=utf8;

-- L2Emu-RT
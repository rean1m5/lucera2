DROP TABLE IF EXISTS `character_mail`;
CREATE TABLE `character_mail` (
  `charId` int(10) NOT NULL DEFAULT '0',
  `letterId` int(11) NOT NULL DEFAULT '0',
  `senderId` int(10) DEFAULT NULL,
  `location` varchar(45) DEFAULT NULL,
  `recipientNames` varchar(45) NOT NULL,
  `subject` text NOT NULL,
  `message` text NOT NULL,
  `sendDate` decimal(20,0) DEFAULT NULL,
  `deleteDate` decimal(20,0) DEFAULT NULL,
  `unread` decimal(1,0) DEFAULT '0',
  PRIMARY KEY (`charId`,`letterId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
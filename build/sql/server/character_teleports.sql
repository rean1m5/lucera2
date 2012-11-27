DROP TABLE IF EXISTS `character_teleports`;
CREATE TABLE `character_teleports` (
  `charId` int(11) NOT NULL,
  `locId` tinyint(4) NOT NULL DEFAULT '1',
  `locName` varchar(50) NOT NULL,
  `acronym` varchar(4) NOT NULL,
  `iconId` tinyint(4) NOT NULL,
  `xCoord` mediumint(9) NOT NULL,
  `yCoord` mediumint(9) NOT NULL,
  `zCoord` mediumint(9) NOT NULL,
  PRIMARY KEY (`charId`,`locId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
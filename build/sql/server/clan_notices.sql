DROP TABLE IF EXISTS `clan_notices`;
CREATE TABLE `clan_notices` (
  `clanID` int(32) NOT NULL default '0',
  `notice` text NOT NULL,
  `enabled` varchar(5) NOT NULL default '',
  PRIMARY KEY  (`clanID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
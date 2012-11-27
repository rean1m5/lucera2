DROP TABLE IF EXISTS `fishing_championship`;
CREATE TABLE `fishing_championship` (
  `PlayerName` varchar(35) NOT NULL,
  `fishLength` float(10,2) NOT NULL,
  `rewarded` int(1) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
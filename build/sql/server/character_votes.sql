DROP TABLE IF EXISTS `character_votes`;
CREATE TABLE `character_votes` (
  `votedate` datetime NOT NULL,
  `charName` varchar(32) NOT NULL DEFAULT '',
  `deamon_name` varchar(32) NOT NULL DEFAULT '',
  KEY `votedate_idx` (`votedate`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
DROP TABLE IF EXISTS `character_friends`;
CREATE TABLE `character_friends` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `friendId` int(10) unsigned NOT NULL DEFAULT '0',
  `friend_name` varchar(35) NOT NULL DEFAULT '',
  PRIMARY KEY (`charId`,`friend_name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
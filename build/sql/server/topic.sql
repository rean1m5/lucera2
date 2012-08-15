DROP TABLE IF EXISTS `topic`;
CREATE TABLE `topic` (
  `topic_id` int(8) NOT NULL DEFAULT '0',
  `topic_forum_id` int(8) NOT NULL DEFAULT '0',
  `topic_name` varchar(255) NOT NULL DEFAULT '',
  `topic_date` decimal(20,0) NOT NULL DEFAULT '0',
  `topic_ownername` varchar(255) NOT NULL DEFAULT '0',
  `topic_ownerid` int(8) NOT NULL DEFAULT '0',
  `topic_type` int(8) NOT NULL DEFAULT '0',
  `topic_reply` int(8) NOT NULL DEFAULT '0',
  PRIMARY KEY (`topic_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
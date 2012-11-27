DROP TABLE IF EXISTS `posts`;
CREATE TABLE `posts` (
  `post_id` int(8) NOT NULL DEFAULT '0',
  `post_owner_name` varchar(255) NOT NULL DEFAULT '',
  `post_ownerid` int(8) NOT NULL DEFAULT '0',
  `post_date` decimal(20,0) NOT NULL DEFAULT '0',
  `post_topic_id` int(8) NOT NULL DEFAULT '0',
  `post_forum_id` int(8) NOT NULL DEFAULT '0',
  `post_txt` text NOT NULL,
  PRIMARY KEY (`post_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
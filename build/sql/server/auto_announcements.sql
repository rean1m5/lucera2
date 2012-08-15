DROP TABLE IF EXISTS `auto_announcements`;
CREATE TABLE `auto_announcements` (
  `id` int(11) NOT NULL,
  `initial` bigint(20) NOT NULL,
  `delay` bigint(20) NOT NULL,
  `cycle` int(11) NOT NULL,
  `memo` text,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
DROP TABLE IF EXISTS `clanhall_siege`;
CREATE TABLE `clanhall_siege` (
  `id` int(11) NOT NULL default '0',
  `name` varchar(40) NOT NULL default '',
  `siege_data` decimal(20,0) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


INSERT INTO `clanhall_siege` VALUES ('21', 'Fortress of Resistance', '1277488800562');
INSERT INTO `clanhall_siege` VALUES ('34', 'Devastated Castle', '1277488800687');
INSERT INTO `clanhall_siege` VALUES ('35', 'Bandit Stronghold', '1277488800953');
INSERT INTO `clanhall_siege` VALUES ('63', 'Wild Beast Farm', '1277488800953');
INSERT INTO `clanhall_siege` VALUES ('64', 'Fortress of Dead', '1277488800796');
DROP TABLE IF EXISTS `character_skills`;
CREATE TABLE `character_skills` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `skill_id` int(11) NOT NULL DEFAULT '0',
  `skill_level` int(3) NOT NULL DEFAULT '1',
  `skill_name` varchar(40) DEFAULT NULL,
  `class_index` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`,`skill_id`,`class_index`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
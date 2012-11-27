DROP TABLE IF EXISTS `character_skills_save`;
CREATE TABLE `character_skills_save` (
  `char_obj_id` int(11) NOT NULL DEFAULT '0',
  `skill_id` int(10) unsigned NOT NULL DEFAULT '0',
  `skill_level` smallint(5) unsigned NOT NULL DEFAULT '0',
  `class_index` smallint(6) NOT NULL DEFAULT '0',
  `end_time` bigint(20) NOT NULL DEFAULT '0',
  `reuse_delay_org` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_obj_id`,`skill_id`,`class_index`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
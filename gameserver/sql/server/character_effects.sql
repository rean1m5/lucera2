DROP TABLE IF EXISTS `character_effects`;
CREATE TABLE `character_effects` (
  `object_id` int(11) NOT NULL,
  `skill_id` int(11) NOT NULL,
  `skill_level` int(11) NOT NULL,
  `effect_count` int(11) NOT NULL,
  `effect_cur_time` int(11) NOT NULL,
  `duration` int(11) NOT NULL,
  `order` int(11) NOT NULL,
  `class_index` int(11) NOT NULL,
  PRIMARY KEY (`object_id`,`skill_id`,`class_index`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

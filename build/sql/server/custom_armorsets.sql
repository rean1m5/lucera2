DROP TABLE IF EXISTS `custom_armorsets`;
CREATE TABLE `custom_armorsets` (
  `id` smallint(5) unsigned NOT NULL auto_increment,
  `chest` smallint(5) unsigned NOT NULL default '0',
  `legs` smallint(5) unsigned NOT NULL default '0',
  `head` smallint(5) unsigned NOT NULL default '0',
  `gloves` smallint(5) unsigned NOT NULL default '0',
  `feet` smallint(5) unsigned NOT NULL default '0',
  `skill_id` smallint(5) unsigned NOT NULL default '0',
  `skill_lvl` tinyint(3) unsigned NOT NULL default '0',
  `skillset_id` smallint(5) unsigned NOT NULL default '0',
  `shield` smallint(5) unsigned NOT NULL default '0',
  `shield_skill_id` smallint(5) unsigned NOT NULL default '0',
  `enchant6skill` smallint(5) unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`,`chest`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
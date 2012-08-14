/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `helper_buff_list` */

DROP TABLE IF EXISTS `helper_buff_list`;

CREATE TABLE `helper_buff_list` (
  `id` int(11) NOT NULL default '0',
  `skill_id` int(10) unsigned NOT NULL default '0',
  `name` varchar(25) NOT NULL default '',
  `skill_level` int(10) unsigned NOT NULL default '0',
  `lower_level` int(10) unsigned NOT NULL default '0',
  `upper_level` int(10) unsigned NOT NULL default '0',
  `owner_class` varchar(10) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `helper_buff_list` */

LOCK TABLES `helper_buff_list` WRITE;

insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (0,4322,'Wind Walk',1,6,41,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (1,4323,'Shield',1,6,41,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (2,4338,'Life Cubic',1,16,34,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (3,4324,'Bless the Body',1,6,41,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (4,4325,'Vampiric Rage',1,6,41,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (5,4326,'Regeneration',1,6,41,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (6,4327,'Haste',1,6,39,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (7,4322,'WindWalk',1,6,41,'Mage');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (8,4323,'Shield',1,6,41,'Mage');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (9,4338,'Life Cubic',1,16,34,'Mage');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (10,4328,'Bless the Soul',1,6,41,'Mage');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (11,4329,'Acumen',1,6,41,'Mage');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (12,4330,'Concentration',1,6,41,'Mage');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (13,4331,'Empower',1,6,41,'Mage');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (14,5632,'Haste',1,40,41,'Fighter');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (17,4322,'Wind Walk',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (18,4323,'Shield',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (20,4324,'Bless the Body',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (21,4328,'Bless the Soul',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (22,4325,'Vampiric Rage',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (23,4329,'Acumen',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (24,4326,'Regeneration',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (25,4330,'Concentration',1,6,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (26,5632,'Haste',1,40,41,'Summon');
insert  into `helper_buff_list`(`id`,`skill_id`,`name`,`skill_level`,`lower_level`,`upper_level`,`owner_class`) values (27,4331,'Empower',1,6,41,'Summon');

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
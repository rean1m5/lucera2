/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - lucera
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `items` */

DROP TABLE IF EXISTS `items`;

CREATE TABLE `items` (
  `owner_id` int(11) default NULL,
  `object_id` int(11) NOT NULL default '0',
  `item_id` int(11) default NULL,
  `count` int(11) default NULL,
  `enchant_level` int(11) default NULL,
  `loc` varchar(10) default NULL,
  `loc_data` int(11) default NULL,
  `time_of_use` int(11) default NULL,
  `custom_type1` int(11) default '0',
  `custom_type2` int(11) default '0',
  `mana_left` decimal(5,0) NOT NULL default '-1',
  `attributes` varchar(50) default '',
  `process` varchar(64) NOT NULL default '',
  `creator_id` int(11) default NULL,
  `first_owner_id` int(11) NOT NULL,
  `creation_time` decimal(16,0) default NULL,
  `data` varchar(128) default NULL,
  PRIMARY KEY  (`object_id`),
  KEY `key_owner_id` (`owner_id`),
  KEY `key_loc` (`loc`),
  KEY `key_item_id` (`item_id`),
  KEY `key_time_of_use` (`time_of_use`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
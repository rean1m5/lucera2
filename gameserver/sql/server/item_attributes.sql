/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `item_attributes` */

DROP TABLE IF EXISTS `item_attributes`;

CREATE TABLE `item_attributes` (
  `itemId` int(11) NOT NULL default '0',
  `augAttributes` int(11) NOT NULL default '-1',
  `augSkillId` int(11) NOT NULL default '-1',
  `augSkillLevel` int(11) NOT NULL default '-1',
  PRIMARY KEY  (`itemId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
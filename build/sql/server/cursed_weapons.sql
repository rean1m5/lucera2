/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `cursed_weapons` */

DROP TABLE IF EXISTS `cursed_weapons`;

CREATE TABLE `cursed_weapons` (
  `itemId` int(11) NOT NULL default '0',
  `charId` int(11) default '0',
  `playerKarma` int(11) default '0',
  `playerPkKills` int(11) default '0',
  `nbKills` int(11) default '0',
  `endTime` decimal(20,0) default '0',
  PRIMARY KEY  (`itemId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `cursed_weapons` */

LOCK TABLES `cursed_weapons` WRITE;

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `custom_droplist` */

DROP TABLE IF EXISTS `custom_droplist`;

CREATE TABLE `custom_droplist` (
  `mobId` int(11) NOT NULL default '0',
  `itemId` int(11) NOT NULL default '0',
  `min` int(11) NOT NULL default '0',
  `max` int(11) NOT NULL default '0',
  `category` int(11) NOT NULL default '0',
  `chance` int(11) NOT NULL default '0',
  PRIMARY KEY  (`mobId`,`itemId`,`category`),
  KEY `key_mobId` (`mobId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `custom_droplist` */

LOCK TABLES `custom_droplist` WRITE;

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
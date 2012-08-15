/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `custom_npcskills` */

DROP TABLE IF EXISTS `custom_npcskills`;

CREATE TABLE `custom_npcskills` (
  `npcid` int(11) NOT NULL default '0',
  `skillid` int(11) NOT NULL default '0',
  `level` int(11) NOT NULL default '0',
  PRIMARY KEY  (`npcid`,`skillid`,`level`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `custom_npcskills` */

LOCK TABLES `custom_npcskills` WRITE;

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
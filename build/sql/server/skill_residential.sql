/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - lucera
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `skill_residential` */

DROP TABLE IF EXISTS `skill_residential`;

CREATE TABLE `skill_residential` (
  `entityId` int(11) NOT NULL,
  `skillId` int(11) NOT NULL default '0',
  `skillLevel` int(11) NOT NULL default '0',
  PRIMARY KEY  (`entityId`,`skillId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `skill_residential` */

LOCK TABLES `skill_residential` WRITE;

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
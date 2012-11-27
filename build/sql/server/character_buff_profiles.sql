/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - lucera
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `character_buff_profiles` */

DROP TABLE IF EXISTS `character_buff_profiles`;

CREATE TABLE `character_buff_profiles` (
  `charId` int(10) unsigned NOT NULL,
  `profileName` varchar(32) NOT NULL,
  `buffGroup` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`charId`,`profileName`,`buffGroup`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `character_buff_profiles` */

LOCK TABLES `character_buff_profiles` WRITE;

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
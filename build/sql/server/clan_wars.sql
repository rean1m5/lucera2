/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `clan_wars` */

DROP TABLE IF EXISTS `clan_wars`;

CREATE TABLE `clan_wars` (
  `clan1` varchar(35) NOT NULL default '',
  `clan2` varchar(35) NOT NULL default '',
  `wantspeace1` decimal(1,0) NOT NULL default '0',
  `wantspeace2` decimal(1,0) NOT NULL default '0'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `clan_wars` */

LOCK TABLES `clan_wars` WRITE;

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
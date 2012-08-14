/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `couples` */

DROP TABLE IF EXISTS `couples`;

CREATE TABLE `couples` (
  `id` int(11) NOT NULL auto_increment,
  `player1Id` int(11) NOT NULL default '0',
  `player2Id` int(11) NOT NULL default '0',
  `maried` varchar(5) default NULL,
  `affiancedDate` decimal(20,0) default '0',
  `weddingDate` decimal(20,0) default '0',
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `couples` */

LOCK TABLES `couples` WRITE;

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
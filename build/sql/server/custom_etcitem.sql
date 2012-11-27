/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `custom_etcitem` */

DROP TABLE IF EXISTS `custom_etcitem`;

CREATE TABLE `custom_etcitem` (
  `item_id` decimal(11,0) NOT NULL default '0',
  `item_display_id` decimal(11,0) NOT NULL default '0',
  `name` varchar(100) default NULL,
  `crystallizable` varchar(5) default NULL,
  `item_type` varchar(14) default NULL,
  `weight` decimal(4,0) default NULL,
  `consume_type` varchar(9) default NULL,
  `material` varchar(11) default NULL,
  `crystal_type` varchar(4) default NULL,
  `duration` decimal(3,0) default NULL,
  `lifetime` int(3) NOT NULL default '-1',
  `price` decimal(11,0) default NULL,
  `crystal_count` int(4) default NULL,
  `sellable` varchar(5) default NULL,
  `dropable` varchar(5) default NULL,
  `destroyable` varchar(5) default NULL,
  `tradeable` varchar(5) default NULL,
  `skill` varchar(70) default '0-0;',
  `html` varchar(5) default 'false',
  PRIMARY KEY  (`item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `custom_etcitem` */

LOCK TABLES `custom_etcitem` WRITE;

insert  into `custom_etcitem`(`item_id`,`item_display_id`,`name`,`crystallizable`,`item_type`,`weight`,`consume_type`,`material`,`crystal_type`,`duration`,`lifetime`,`price`,`crystal_count`,`sellable`,`dropable`,`destroyable`,`tradeable`,`skill`,`html`) values ('15000','6673','Gold Coin','false','material','0','stackable','steel','none','100',-1,'0',0,'false','false','false','true','0-0','false');

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
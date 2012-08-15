/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - lutest
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `character_items` */

DROP TABLE IF EXISTS `character_items`;

CREATE TABLE `character_items` (
  `owner_id` int(11) default NULL,
  `item_id` int(11) default NULL,
  `count` bigint(20) default '1',
  `enchant_level` int(11) default '0'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `character_items` */

insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268539967,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268576573,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268535223,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268564747,4037,4,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268561422,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268576573,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268632146,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268517849,4037,4,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268576573,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268514800,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268539703,4037,4,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268594646,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268539967,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268522501,4037,4,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268522445,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268519446,4037,4,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268522501,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268519446,4037,4,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268535223,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268576573,4037,3,0);
insert  into `character_items`(`owner_id`,`item_id`,`count`,`enchant_level`) values (268519446,4037,4,0);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - lucera
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `custom_npcbuffer` */

DROP TABLE IF EXISTS `custom_npcbuffer`;

CREATE TABLE `custom_npcbuffer` (
  `npc_id` int(6) NOT NULL default '0',
  `skill_id` int(6) NOT NULL default '0',
  `skill_level` int(6) NOT NULL default '1',
  `skill_fee_id` int(6) NOT NULL default '0',
  `skill_fee_amount` int(6) NOT NULL default '0',
  `buff_group` int(6) NOT NULL default '0',
  PRIMARY KEY  (`npc_id`,`skill_id`,`buff_group`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

/*Data for the table `custom_npcbuffer` */

LOCK TABLES `custom_npcbuffer` WRITE;

insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,264,1,57,20000,264);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,265,1,57,20000,265);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,266,1,57,20000,266);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,267,1,57,20000,267);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,268,1,57,20000,268);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,269,1,57,20000,269);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,270,1,57,20000,270);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,271,1,57,20000,271);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,272,1,57,20000,272);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,273,1,57,20000,273);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,274,1,57,20000,274);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,275,1,57,20000,275);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,276,1,57,20000,276);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,277,1,57,20000,277);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,304,1,57,20000,304);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,305,1,57,20000,305);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,306,1,57,20000,306);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,307,1,57,20000,307);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,308,1,57,20000,308);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,309,1,57,20000,309);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,310,1,57,20000,310);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,311,1,57,20000,311);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,349,1,57,20000,349);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,363,1,57,20000,363);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,364,1,57,20000,364);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,366,1,57,20000,366);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,367,1,57,20000,367);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1032,3,57,20000,1032);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1033,3,57,20000,1033);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1035,4,57,20000,1035);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1036,2,57,20000,1036);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1040,3,57,20000,1040);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1043,1,57,20000,1043);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1044,3,57,20000,1044);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1045,6,57,20000,1045);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1047,4,57,20000,1047);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1048,6,57,20000,1048);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1059,3,57,20000,1059);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1062,2,57,20000,1062);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1068,3,57,20000,1068);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1077,3,57,20000,1077);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1078,6,57,20000,1078);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1085,3,57,20000,1085);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1086,2,57,20000,1086);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1182,3,57,20000,1182);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1189,3,57,20000,1189);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1191,3,57,20000,1191);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1204,2,57,20000,1204);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1240,3,57,20000,1240);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1242,3,57,20000,1242);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1243,6,57,20000,1243);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1268,4,57,20000,1268);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1285,1,57,20000,1285);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1286,1,57,20000,1286);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1287,1,57,20000,1287);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1303,2,57,20000,1303);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1397,3,57,20000,1397);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1007,3,57,20000,1007);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1006,3,57,20000,1006);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1009,3,57,20000,1009);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1229,18,57,20000,1229);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1252,3,57,20000,1252);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1253,3,57,20000,1253);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1310,4,57,20000,1310);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1251,2,57,20000,1251);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1308,3,57,20000,1308);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1309,3,57,20000,1309);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1390,3,57,20000,1390);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1391,3,57,20000,1391);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1362,1,57,20000,1362);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1363,1,57,20000,1363);
insert  into `custom_npcbuffer`(`npc_id`,`skill_id`,`skill_level`,`skill_fee_id`,`skill_fee_amount`,`buff_group`) values (50000,1413,1,57,20000,1413);

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
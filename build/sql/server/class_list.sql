/*
SQLyog Enterprise - MySQL GUI v7.15 
MySQL - 5.0.27-community-nt : Database - l2it
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

/*Table structure for table `class_list` */

DROP TABLE IF EXISTS `class_list`;

CREATE TABLE `class_list` (
  `class_name` varchar(20) NOT NULL default '',
  `id` int(10) unsigned NOT NULL default '0',
  `parent_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Data for the table `class_list` */

LOCK TABLES `class_list` WRITE;

insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Fighter',0,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Warrior',1,0);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Gladiator',2,1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Duelist',88,2);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Warlord',3,1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Dreadnought',89,3);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Knight',4,0);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Paladin',5,4);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_PhoenixKnight',90,5);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_DarkAvenger',6,4);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_HellKnight',91,6);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Rogue',7,0);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_TreasureHunter',8,7);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Adventurer',93,8);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Hawkeye',9,7);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Sagittarius',92,9);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Mage',10,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Wizard',11,10);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Sorceror',12,11);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Archmage',94,12);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Necromancer',13,11);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Soultaker',95,13);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Warlock',14,11);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_ArcanaLord',96,14);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Cleric',15,10);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Bishop',16,15);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Cardinal',97,16);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Prophet',17,15);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('H_Hierophant',98,17);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_Fighter',18,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_Knight',19,18);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_TempleKnight',20,19);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_EvaTemplar',99,20);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_SwordSinger',21,19);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_SwordMuse',100,21);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_Scout',22,18);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_PlainsWalker',23,22);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_WindRider',101,23);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_SilverRanger',24,22);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_MoonlightSentinel',102,24);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_Mage',25,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_Wizard',26,25);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_SpellSinger',27,26);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_MysticMuse',103,27);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_ElementalSummoner',28,26);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_ElementalMaster',104,28);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_Oracle',29,25);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_Elder',30,29);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('E_EvaSaint',105,30);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_Fighter',31,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_PaulusKnight',32,31);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_ShillienKnight',33,32);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_ShillienTemplar',106,33);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_BladeDancer',34,32);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_SpectralDancer',107,34);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_Assassin',35,31);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_AbyssWalker',36,35);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_GhostHunter',108,36);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_PhantomRanger',37,35);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_GhostSentinel',109,37);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_Mage',38,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_DarkWizard',39,38);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_Spellhowler',40,39);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_StormScreamer',110,40);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_PhantomSummoner',41,39);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_SpectralMaster',111,41);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_ShillienOracle',42,38);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_ShillienElder',43,42);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('DE_ShillienSaint',112,43);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Fighter',44,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Raider',45,44);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Destroyer',46,45);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Titan',113,46);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Monk',47,44);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Tyrant',48,47);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_GrandKhauatari',114,48);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Mage',49,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Shaman',50,49);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Overlord',51,50);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Dominator',115,51);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Warcryer',52,50);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('O_Doomcryer',116,52);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('D_Fighter',53,-1);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('D_Scavenger',54,53);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('D_BountyHunter',55,54);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('D_FortuneSeeker',117,55);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('D_Artisan',56,53);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('D_Warsmith',57,56);
insert  into `class_list`(`class_name`,`id`,`parent_id`) values ('D_Maestro',118,57);

UNLOCK TABLES;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
DROP TABLE IF EXISTS `custom_npc`;

CREATE TABLE `custom_npc` (
  `id` decimal(11,0) NOT NULL default '0',
  `idTemplate` int(11) NOT NULL default '0',
  `name` varchar(200) default NULL,
  `serverSideName` int(1) default '0',
  `title` varchar(45) default '',
  `race` int(11) NOT NULL default '5',
  `serverSideTitle` int(1) default '0',
  `collision_radius` decimal(5,2) default NULL,
  `collision_height` decimal(5,2) default NULL,
  `level` decimal(2,0) default NULL,
  `sex` varchar(6) default NULL,
  `type` varchar(20) default NULL,
  `attackrange` int(11) default NULL,
  `hp` decimal(8,0) default NULL,
  `mp` decimal(8,0) default NULL,
  `hpreg` decimal(8,2) default NULL,
  `mpreg` decimal(5,2) default NULL,
  `str` decimal(7,0) default NULL,
  `con` decimal(7,0) default NULL,
  `dex` decimal(7,0) default NULL,
  `int` decimal(7,0) default NULL,
  `wit` decimal(7,0) default NULL,
  `men` decimal(7,0) default NULL,
  `exp` decimal(9,0) default NULL,
  `sp` decimal(8,0) default NULL,
  `patk` decimal(5,0) default NULL,
  `pdef` decimal(5,0) default NULL,
  `matk` decimal(5,0) default NULL,
  `mdef` decimal(5,0) default NULL,
  `atkspd` decimal(3,0) default NULL,
  `aggro` decimal(6,0) default NULL,
  `matkspd` decimal(4,0) default NULL,
  `rhand` decimal(8,0) default NULL,
  `lhand` decimal(8,0) default NULL,
  `armor` decimal(1,0) default NULL,
  `walkspd` decimal(3,0) default NULL,
  `runspd` decimal(3,0) default NULL,
  `faction_id` varchar(40) default NULL,
  `faction_range` decimal(4,0) default NULL,
  `isUndead` int(11) default '0',
  `absorb_level` decimal(2,0) default '0',
  `absorb_type` enum('FULL_PARTY','LAST_HIT','PARTY_ONE_RANDOM') NOT NULL default 'LAST_HIT',
  `ss` int(4) default '0',
  `bss` int(4) default '0',
  `ss_rate` int(3) default '0',
  `AI` varchar(8) default 'fighter',
  `drop_herbs` enum('true','false') NOT NULL default 'false',
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50000',30175,'Buffer',1,'NpcBuffer',5,1,'8.00','25.00','70','female','L2NpcBuffer',40,'3862','1493',NULL,NULL,'40','43','30','21','35','10','0','0','1314','470','780','382','278','0','253','0','0','0','80','120',NULL,'0',1,'0','LAST_HIT',0,0,0,'balanced','false');
insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50003',30006,'Gatekeeper',1,'Global Gatekeeper',5,1,'8.00','25.00','70','female','L2Teleporter',40,'3862','1493',NULL,NULL,'40','43','30','21','35','10','0','0','1314','470','780','382','278','0','253','0','0','0','80','120',NULL,'0',1,'0','LAST_HIT',0,0,0,'balanced','false');
insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50004',31255,'E.Manager',1,'Event Manager',5,1,'9.00','16.00','70','male','L2Npc',40,'3862','1493','11.85','2.78','40','43','30','21','20','10','0','0','1314','470','780','382','278','0','333','0','0','0','88','132',NULL,'0',0,'0','LAST_HIT',0,0,0,'balanced','false');
insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50010',30082,'Gatis',1,'Jail Manager',5,1,'8.00','23.00','70','male','L2JailManager',40,'3862','1493',NULL,NULL,'40','43','30','21','35','10','0','0','1314','470','780','382','278','0','999','0','0','0','80','120',NULL,'0',1,'0','LAST_HIT',0,0,0,'balanced','false');
insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50014',30175,'Andromeda',1,'Wedding Priest',5,1,'8.00','23.00','70','female','L2WeddingManager',40,'3862','1493','11.85','2.78','40','43','30','21','35','10','0','0','1314','470','780','382','278','0','253','0','0','0','80','120',NULL,'0',1,'0','LAST_HIT',0,0,0,'balanced','false');
insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50015',31228,'Event Cat',1,'Class Master',5,1,'9.00','16.00','70','male','L2ClassMaster',40,'3862','1493','11.85','2.78','40','43','30','21','20','10','0','0','1314','470','780','382','278','0','333','0','0','0','88','132',NULL,'0',0,'0','LAST_HIT',0,0,0,'balanced','false');

-- Auction NPC:
insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50018', '31125', 'Auction', '1', 'Author: Ro0TT', '5', '1', '9.00', '16.00', '70', 'male', 'L2Npc', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '88', '132', '', '0', '0', '0', 'LAST_HIT', '0', '0', '0', 'balanced', 'false');

-- Death Note NPC:
insert  into `custom_npc`(`id`,`idTemplate`,`name`,`serverSideName`,`title`,`race`,`serverSideTitle`,`collision_radius`,`collision_height`,`level`,`sex`,`type`,`attackrange`,`hp`,`mp`,`hpreg`,`mpreg`,`str`,`con`,`dex`,`int`,`wit`,`men`,`exp`,`sp`,`patk`,`pdef`,`matk`,`mdef`,`atkspd`,`aggro`,`matkspd`,`rhand`,`lhand`,`armor`,`walkspd`,`runspd`,`faction_id`,`faction_range`,`isUndead`,`absorb_level`,`absorb_type`,`ss`,`bss`,`ss_rate`,`AI`,`drop_herbs`) values ('50016', '31125', 'Killer Manager', '1', 'Author: Ro0TT', '5', '1', '9.00', '16.00', '70', 'male', 'L2Npc', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '88', '132', '', '0', '0', '0', 'LAST_HIT', '0', '0', '0', 'balanced', 'false');

-- Fight Club NPC:
INSERT INTO `custom_npc` VALUES ('50022', '31125', 'Fight Club', '1', 'Author: Ro0TT', '5', '1', '9.00', '16.00', '70', 'male', 'L2Npc', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '88', '132', '', '0', '0', '0', 'LAST_HIT', '0', '0', '0', 'balanced', 'false');

DROP TABLE IF EXISTS `pets`;
CREATE TABLE `pets` (
  `item_obj_id` decimal(11,0) NOT NULL DEFAULT '0',
  `name` varchar(16) DEFAULT NULL,
  `level` decimal(11,0) DEFAULT NULL,
  `curHp` decimal(18,0) DEFAULT NULL,
  `curMp` decimal(18,0) DEFAULT NULL,
  `exp` decimal(20,0) DEFAULT NULL,
  `sp` decimal(11,0) DEFAULT NULL,
  `fed` decimal(11,0) DEFAULT NULL,
  `weapon` int(5) DEFAULT NULL,
  `armor` int(5) DEFAULT NULL,
  `jewel` int(5) DEFAULT NULL,
  PRIMARY KEY (`item_obj_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
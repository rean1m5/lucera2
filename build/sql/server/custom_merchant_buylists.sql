DROP TABLE IF EXISTS `custom_merchant_buylists`;
CREATE TABLE `custom_merchant_buylists` (
  `item_id` decimal(9,0) NOT NULL default '0',
  `price` decimal(11,0) NOT NULL default '0',
  `shop_id` decimal(9,0) NOT NULL default '0',
  `order` decimal(4,0) NOT NULL default '0',
  `count` int(11) NOT NULL default '-1',
  `currentCount` int(11) NOT NULL default '-1',
  `time` int(11) NOT NULL default '0',
  `savetimer` decimal(20,0) NOT NULL default '0',
  PRIMARY KEY  (`shop_id`,`order`),
  UNIQUE KEY `shop_item_idx` (`shop_id`,`item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
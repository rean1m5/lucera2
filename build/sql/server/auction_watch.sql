DROP TABLE IF EXISTS `auction_watch`;
CREATE TABLE `auction_watch` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `auctionId` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`,`auctionId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
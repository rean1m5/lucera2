SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `mails`
-- ----------------------------
DROP TABLE IF EXISTS `mails`;
CREATE TABLE `mails` (
  `mailId` int(11) NOT NULL,
  `sendDate` bigint(30) NOT NULL,
  `sender` int(11) NOT NULL,
  `taker` int(11) NOT NULL,
  `message` text NOT NULL,
  `protection` text NOT NULL,
  `refound` int(1) DEFAULT '0',
  PRIMARY KEY (`mailId`,`sender`,`taker`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of mails
-- ----------------------------

-- ----------------------------
-- Table structure for `mails_attachments`
-- ----------------------------
DROP TABLE IF EXISTS `mails_attachments`;
CREATE TABLE `mails_attachments` (
  `mailId` int(11) NOT NULL,
  `objId` int(11) NOT NULL,
  PRIMARY KEY (`mailId`,`objId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of mails_attachments
-- ----------------------------

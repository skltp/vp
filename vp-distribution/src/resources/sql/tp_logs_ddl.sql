CREATE DATABASE /*!32312 IF NOT EXISTS*/ `tp_logs` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `tp_logs`;

DROP TABLE IF EXISTS `session`;
CREATE TABLE `session` (
  `session_id` varchar(255) NOT NULL,
  `sender_id` varchar(45) DEFAULT NULL,
  `contract` varchar(127) DEFAULT NULL,
  `receiver` varchar(127) DEFAULT NULL,
  `timestamp` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `session_waypoint`;
CREATE TABLE `session_waypoint` (
  `session_waypoint_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `waypoint` varchar(45) NOT NULL,
  `payload` text,
  `session_id` varchar(255) NOT NULL,
  `timestamp` bigint(20) NOT NULL DEFAULT '0',
  `riv_version` varchar(45) NOT NULL,
  PRIMARY KEY (`session_waypoint_id`),
  KEY `fk_session_waypoint_1` (`session_id`),
  CONSTRAINT `fk_session_waypoint_1` FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

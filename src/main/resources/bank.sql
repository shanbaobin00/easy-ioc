
CREATE DATABASE IF NOT EXISTS `bank`;
USE `bank`;

CREATE TABLE IF NOT EXISTS `account` (
  `cardNo` varchar(50) DEFAULT NULL,
  `name` varchar(50) DEFAULT NULL,
  `money` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `account` (`cardNo`, `name`, `money`) VALUES
	('6029621011001', '韩梅梅', 10000),
	('6029621011000', '李大雷', 10000);


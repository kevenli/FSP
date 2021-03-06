

CREATE TABLE `applications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `app_key` varchar(50) NOT NULL,
  `app_secret` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table `worker_status`(
  `id` int not null primary key,
  `name` varchar(20) not null
) ENGINE = InnoDB DEFAULT CHARSET=utf8;

insert into `worker_status`(`id`,`name`) values(0, 'Unknown'), (1,'Online'),(2,'Offline');

CREATE TABLE `workers` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `application_id` BIGINT(20) NOT NULL,
  `client_worker_id` VARCHAR(50) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `schedule` VARCHAR(50) NOT NULL,
  `timeout` INT NOT NULL,
  `status` INT NOT NULL,
  `createtime` DATETIME NOT NULL,
  `updatetime` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_application_worker` (`application_id`,`client_worker_id`),
  CONSTRAINT `fk_worker_status` FOREIGN KEY (`status`) REFERENCES `worker_status` (`id`)
) ENGINE = INNODB DEFAULT CHARSET=utf8;

create table `task_status`(
  `id` int not null primary key,
  `name` varchar(20) not null
) ENGINE = InnoDB DEFAULT CHARSET=utf8;

insert  into `task_status`(`id`,`name`) values (0, 'Unknown'),(1,'NotStart'),(2,'Start'),(3,'Running'),(4,'Success'),(5,'Failed'),(6,'Timeout');

create table `tasks` (
  `id` bigint(20) not null auto_increment,
  `worker_id` bigint(20) not null,
  `create_time` datetime not null,
  `start_time` datetime null,
  `complete_time` datetime null,
  `update_time` datetime null,
  `status` int not null,
  primary key (`id`),
  CONSTRAINT `fk_task_status` FOREIGN KEY (`status`) REFERENCES `task_status` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8;




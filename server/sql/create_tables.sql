CREATE TABLE `applications` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `app_key` varchar(50) NOT NULL,
  `app_secret` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into applications
(id, name, app_key, app_secret)
values
(1, 'testapp', 'abc', '123');

CREATE TABLE `sessions` (
  `id` int(11) NOT NULL,
  `application_id` int(11) NOT NULL,
  `client_ip` varchar(15) NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table `tasks` (
  `id` int(11) not null auto_increment,
  `application_id` int(11) not null,
  `client_task_id` varchar(50) not null,
  `execute_time` varchar(50) not null,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tasks_application_id_task_client_id` (`application_id`,`client_task_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table `task_instances` (
  `id` char(36) not null,
  `task_id` int not null,
  `session_id` int not null,
  `fire_time` datetime not null,
  `start_time` datetime null,
  `update_time` datetime null,
  `complete_time` datetime null,
  `status` int not null,
  PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table `task_status`(
  `id` int not null primary key,
  `name` varchar(20) not null
) ENGINE = InnoDB DEFAULT CHARSET=utf8;

insert  into `task_status`(`id`,`name`) values (0, 'Unknown'),(1,'NotStart'),(2,'Start'),(3,'Running'),(4,'Success'),(5,'Failed'),(6,'Timeout');



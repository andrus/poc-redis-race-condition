use database;

CREATE TABLE `database`.scores (
  `id` VARCHAR (255),
  score TEXT,
  PRIMARY KEY (`id`)
) COMMENT='This is a table to hold game scores - will be cached down the line';
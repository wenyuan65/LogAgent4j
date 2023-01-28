create table `player_interface`(
	`time` DateTime,
	`game` String,
	`server` String,
	`user_id` String,
	`player_id` Int64,
	`player_name` String,
	`command` String,
	`params` String,
	`cost_time` UInt32,
	`yx` String,
	`platform` String,
	`version` String
) engine=MergeTree
partition by toYYYYMMDD(`time`)
order by (`time`, `game`, `player_id`, `yx`)


create table `player_login`(
	`time` DateTime,
	`game` String,
	`server` String,
	`user_id` String,
	`player_id` Int64,
	`player_name` String,
	`first_login` UInt8,
	`yx` String,
	`platform` String,
	`version` String
) engine=MergeTree
partition by toYYYYMMDD(`time`)
order by (`time`, `game`, `player_id`, `yx`)


create table `player_register`(
	`time` DateTime,
	`game` String,
	`server` String,
	`user_id` String,
	`player_id` Int64,
	`player_name` String,
	`yx` String,
	`platform` String,
	`version` String
) engine=MergeTree
partition by toYYYYMMDD(`time`)
order by (`time`, `game`, `player_id`, `yx`)


create table `player_action`(
	`time` DateTime,
	`game` String,
	`server` String,
	`user_id` String,
	`player_id` Int64,
	`player_name` String,
	`action` String,
	`value` String,
	`param1` String,
	`param2` String,
	`param3` String,
	`param4` String,
	`param5` String,
	`param6` String,
) engine=MergeTree
partition by toYYYYMMDD(`time`)
order by (`game`, `action`, `time`, `player_id`)
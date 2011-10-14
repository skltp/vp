use tp_logs;

alter table session 
	add column error varchar(8) default '',
	add column error_description varchar(255) default '',
	add column technical_error_description text default '';
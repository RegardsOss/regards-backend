alter table t_cached_file add column mime_type varchar(255) not null default 'application/octet-stream';
alter table t_cached_file add column filename varchar(255);

create table t_default_download_quota_limits (id int8 not null, max_quota int8 not null default -1, rate_limit int8 not null default -1, primary key (id));
create sequence seq_default_download_quota_limits start 1 increment 50;
alter table t_default_download_quota_limits add constraint default_quota_range_check check (max_quota > -2);
alter table t_default_download_quota_limits add constraint default_rate_range_check check (rate_limit > -2);
insert into t_default_download_quota_limits (id, max_quota, rate_limit) values (0, -1, -1);

create table t_user_download_quota_limits (id int8 not null, email varchar(128) not null, max_quota int8 not null default -1, rate_limit int8 not null default -1, primary key (id));
create sequence seq_download_quota_limits start 1 increment 50;
alter table t_user_download_quota_limits add constraint uk_download_quota_limits_email unique (email);
alter table t_user_download_quota_limits add constraint quota_range_check check (max_quota > -2);
alter table t_user_download_quota_limits add constraint rate_range_check check (rate_limit > -2);

create table t_user_download_rate_gauge (id int8 not null, instance_id varchar(128) not null, email varchar(128) not null, gauge int8 not null, expiry timestamp not null, primary key (id));
create sequence seq_download_rate_gauge start 1 increment 50;
alter table t_user_download_rate_gauge add constraint uk_download_rate_gauge_instance_email unique (instance_id, email);

create table t_user_download_quota_counter (id int8 not null, instance_id varchar(128) not null, email varchar(128) not null, counter int8 not null, primary key (id));
create sequence seq_download_quota_counter start 1 increment 50;
alter table t_user_download_quota_counter add constraint uk_download_quota_counter_instance_email unique (instance_id, email);

alter table t_cache_file add column type varchar(255);

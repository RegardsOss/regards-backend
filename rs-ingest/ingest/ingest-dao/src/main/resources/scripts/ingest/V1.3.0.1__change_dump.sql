create table t_dump_configuration (id int8 not null, active_module boolean not null, cron_trigger varchar(1000) not null, dump_location varchar(260), last_dump_req_date timestamp);
alter table t_dump_configuration add constraint uk_t_dump_configuration unique (id);
alter table t_request add dump_location varchar(260);
alter table t_request add previous_dump_date timestamp;

alter table t_aip drop column manifest_locations;
alter table t_aip drop column checksum;
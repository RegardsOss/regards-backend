-- Delete columns related to old metadata processing
alter table t_aip drop column manifest_locations;
alter table t_aip drop column checksum;

-- Add dump functioning to store metadata
create table t_ingest_dump_settings (id int8 not null, active_module boolean not null, cron_trigger varchar(1000) not null, dump_location varchar(260), last_dump_req_date timestamp);
alter table t_ingest_dump_settings add constraint uk_t_ingest_dump_settings unique (id);
alter table t_request add dump_location varchar(260);
alter table t_request add previous_dump_date timestamp;

-- Add notification settings to handle optional notifications on aips
create table t_aip_notification_settings (id int8 not null, active_notifications boolean not null default false);
alter table t_aip_notification_settings add constraint uk_t_aip_notification_settings unique (id);
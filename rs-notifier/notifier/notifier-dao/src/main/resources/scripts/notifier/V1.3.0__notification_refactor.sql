-- change action type
ALTER TABLE t_notification_action ALTER COLUMN action TYPE jsonb USING to_jsonb(action);
-- Rename notification column
ALTER TABLE t_notification_action RENAME COLUMN action TO metadata;
ALTER TABLE t_notification_action RENAME COLUMN element TO payload;
ALTER TABLE t_notification_action RENAME COLUMN action_date TO request_date;
-- add requestId
ALTER TABLE t_notification_action ADD COLUMN request_id varchar(36);
-- rename table
ALTER TABLE t_notification_action RENAME TO t_notification_request;

-- add association tables on notification requests

create table ta_notif_request_recipients_toschedule (notification_request_id int8 not null, recipient_id int8 not null, primary key (notification_request_id, recipient_id));
alter table ta_notif_request_recipients_toschedule add constraint fk_notification_request_id_recipients_toschedule foreign key (notification_request_id) references t_notification_request;
alter table ta_notif_request_recipients_toschedule add constraint fk_notification_request_recipients_toschedule_id foreign key (recipient_id) references t_plugin_configuration;
create table ta_notif_request_recipients_scheduled (notification_request_id int8 not null, recipient_id int8 not null, primary key (notification_request_id, recipient_id));
alter table ta_notif_request_recipients_scheduled add constraint fk_notification_request_id_recipients_scheduled foreign key (notification_request_id) references t_notification_request;
alter table ta_notif_request_recipients_scheduled add constraint fk_notification_request_recipients_scheduled_id foreign key (recipient_id) references t_plugin_configuration;
create table ta_notif_request_recipients_error (notification_request_id int8 not null, recipient_id int8 not null, primary key (notification_request_id, recipient_id));
alter table ta_notif_request_recipients_error add constraint fk_notification_request_id_recipients_error foreign key (notification_request_id) references t_notification_request;
alter table ta_notif_request_recipients_error add constraint fk_notification_request_recipients_error_id foreign key (recipient_id) references t_plugin_configuration;

-- change action type
ALTER TABLE t_notification_action ALTER COLUMN action TYPE jsonb USING to_jsonb(action);
-- Rename notification column
ALTER TABLE t_notification_action RENAME COLUMN action TO metadata;
ALTER TABLE t_notification_action RENAME COLUMN element TO payload;
ALTER TABLE t_notification_action RENAME COLUMN action_date TO request_date;
-- add requestId & requestOwner
ALTER TABLE t_notification_action ADD COLUMN request_id varchar(36);
ALTER TABLE t_notification_action ADD COLUMN request_owner varchar(128);
-- add version column for optimistic locking
ALTER TABLE t_notification_action ADD COLUMN version int8;
-- rename table
ALTER TABLE t_notification_action RENAME TO t_notification_request;

-- add association tables on notification requests
-- to schedule
CREATE TABLE ta_notif_request_recipients_toschedule (notification_request_id int8 not null, recipient_id int8 not null, primary key (notification_request_id, recipient_id));
ALTER TABLE ta_notif_request_recipients_toschedule ADD CONSTRAINT fk_notification_request_id_recipients_toschedule foreign key (notification_request_id) references t_notification_request;
ALTER TABLE ta_notif_request_recipients_toschedule ADD CONSTRAINT fk_notification_request_recipients_toschedule_id foreign key (recipient_id) references t_plugin_configuration;
-- scheduled
CREATE TABLE ta_notif_request_recipients_scheduled (notification_request_id int8 not null, recipient_id int8 not null, primary key (notification_request_id, recipient_id));
ALTER TABLE ta_notif_request_recipients_scheduled ADD CONSTRAINT fk_notification_request_id_recipients_scheduled foreign key (notification_request_id) references t_notification_request;
ALTER TABLE ta_notif_request_recipients_scheduled ADD CONSTRAINT fk_notification_request_recipients_scheduled_id foreign key (recipient_id) references t_plugin_configuration;
-- errors
CREATE TABLE ta_notif_request_recipients_error (notification_request_id int8 not null, recipient_id int8 not null, primary key (notification_request_id, recipient_id));
ALTER TABLE ta_notif_request_recipients_error ADD CONSTRAINT fk_notification_request_id_recipients_error foreign key (notification_request_id) references t_notification_request;
ALTER TABLE ta_notif_request_recipients_error ADD CONSTRAINT fk_notification_request_recipients_error_id foreign key (recipient_id) references t_plugin_configuration;
-- rules to match
CREATE TABLE ta_notif_request_rules_to_match (notification_request_id int8 not null, rule_id int8 not null, primary key (notification_request_id, rule_id));
ALTER TABLE ta_notif_request_rules_to_match ADD CONSTRAINT fk_notification_request_id_rules_to_match foreign key (notification_request_id) references t_notification_request;
ALTER TABLE ta_notif_request_rules_to_match ADD CONSTRAINT fk_notification_request_rules_to_match_id foreign key (rule_id) references t_rule;

-- Add indexes on t_notification_request
CREATE INDEX idx_t_notification_request_version ON t_notification_request(version);
CREATE INDEX idx_t_notification_request_state ON t_notification_request(state);
CREATE INDEX idx_t_notification_request_request_id ON t_notification_request(request_id);

CREATE INDEX idx_ta_notif_request_recipients_toschedule_recipient_id ON ta_notif_request_recipients_toschedule(recipient_id);
CREATE INDEX idx_ta_notif_request_recipients_scheduled_recipient_id ON ta_notif_request_recipients_scheduled(recipient_id);
CREATE INDEX idx_ta_notif_request_recipients_error_recipient_id ON ta_notif_request_recipients_error(recipient_id);
CREATE INDEX idx_ta_notif_request_rules_to_match_rule_id ON ta_notif_request_rules_to_match(rule_id);
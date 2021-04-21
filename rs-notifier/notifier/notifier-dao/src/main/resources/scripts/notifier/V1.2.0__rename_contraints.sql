-- Rename constraints
alter table ta_rule_recipients drop constraint if exists UK_qml46c9btebccwqc5vtgbqovs;
alter table ta_rule_recipients
add constraint uk_recipient_id unique (recipient_id);

alter table ta_rule_recipients drop constraint if exists FKegd782tj6jts8vmp18m332lp9;
alter table ta_rule_recipients
add constraint fk_recipient_id foreign key (recipient_id) references t_plugin_configuration;

alter table ta_rule_recipients drop constraint if exists FK3qln4h2mro8y8t6ede2k2hbb3;
alter table ta_rule_recipients
add constraint fk_rule_id foreign key (rule_id) references t_rule;

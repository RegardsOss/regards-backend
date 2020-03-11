/* Plugins */
create table t_plugin_configuration (id int8 not null, bid varchar(36) NOT null, active boolean, parameters jsonb, label varchar(255), pluginId varchar(255) not null, priorityOrder int4 not null, version varchar(255) not null, icon_url varchar(255), primary key (id));
create index idx_plugin_configuration on t_plugin_configuration (pluginId);
create index idx_plugin_configuration_label on t_plugin_configuration (label);
CREATE INDEX idx_plugin_configuration_bid on t_plugin_configuration (bid);
ALTER TABLE t_plugin_configuration ADD CONSTRAINT uk_plugin_bid UNIQUE (bid);
create sequence seq_plugin_conf start 1 increment 50;

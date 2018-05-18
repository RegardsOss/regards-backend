create table ta_sip_errors (sip_entity_id int8 not null, error varchar(255));
alter table ta_sip_errors add constraint fk_errors_sip_entity_id foreign key (sip_entity_id) references t_sip;

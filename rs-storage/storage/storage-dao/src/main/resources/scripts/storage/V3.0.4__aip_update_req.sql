create table t_aip_update_req (id int8 not null, json_aip jsonb, aip_id varchar(128), update_message varchar(512), primary key (id));
create index idx_aip_update_req_ip_id on t_aip_update_req (aip_id);
alter table t_aip_update_req add constraint uk_aip_update_req_ipId unique (aip_id);
create sequence seq_aip_update_req start 1 increment 50;


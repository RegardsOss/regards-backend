-- Add provider id
drop index idx_aip_ip_id;
alter table t_aip drop constraint uk_aip_ipId
alter table t_aip rename column ip_id to aip_id;
alter table t_aip add column provider_id VARCHAR(100);
-- Recreate index
create index idx_aip_ip_id on t_aip(aip_id);
-- Recreate constraint
alter table t_aip add constraint uk_aip_ipId unique (aip_id);

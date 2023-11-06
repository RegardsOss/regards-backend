alter table t_aip
    add column if not exists dissemination_status varchar(32) DEFAULT 'NONE' NOT NULL ,
    add column if not exists dissemination_infos jsonb;

create index if not exists idx_aip_dissemination_status on t_aip (dissemination_status);
create index if not exists idx_aip_creation_date on t_aip(creation_date);
create index if not exists idx_aip_dissemination_status_and_creation_date on t_aip(dissemination_status,creation_date);
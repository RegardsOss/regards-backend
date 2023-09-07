alter table t_aip
    add column if not exists dissemination_status varchar(32),
    add column if not exists dissemination_infos jsonb;

create index if not exists idx_aip_dissementation_status on t_aip (dissemination_status);
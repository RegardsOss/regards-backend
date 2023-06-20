create table t_glacier_archive
(
    id       int8 not null,
    storage  varchar(128),
    url      varchar(2048),
    checksum varchar(128),
    size_ko  int8,
    primary key (id)
);
alter table t_glacier_archive
    add constraint uk_t_glacier_archive_storage_url unique (storage, url);
create sequence seq_glacier_archive start 1 increment 50;

create index idx_glacier_storage_url on t_glacier_archive (storage, url);
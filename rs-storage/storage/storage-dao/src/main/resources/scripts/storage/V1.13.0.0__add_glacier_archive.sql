create table t_glacier_archive
(
    id       int8 not null,
    url      varchar(128),
    checksum varchar(128),
    size_ko  int8,
    primary key (id)
);
alter table t_glacier_archive
    add constraint uk_t_glacier_archive_url unique (url);
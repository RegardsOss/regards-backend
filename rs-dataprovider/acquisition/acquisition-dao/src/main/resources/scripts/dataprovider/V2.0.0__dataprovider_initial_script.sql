-- Acquisition chain
create table t_acquisition_chain (
    id int8 not null,
    active boolean,
    comment text,
    dataSetIpId varchar(255),
    label varchar(255) not null,
    last_date_activation timestamp,
    period int8,
    running boolean,
    session varchar(255),
    checkfile_conf_id int8,
    generatesip_conf_id int8,
    meta_product_id int8,
    postprocesssip_conf_id int8,
    scan_conf_id int8,
    primary key (id)
);
create table t_acquisition_exec_chain (
    id int8 not null,
    session varchar(255),
    start_date timestamp,
    stop_date timestamp,
    chain_id int8,
    primary key (id)
);
create index idx_acq_chain_label on t_acquisition_chain (label);
alter table
    t_acquisition_chain
add
    constraint uk_acq_chain_label unique (label);create index idx_acq_exec_session on t_acquisition_exec_chain (session);
alter table
    t_acquisition_exec_chain
add
    constraint uk_acq_exec_session unique (session);
    
-- Metadata : describe product pattern
create table t_acquisition_meta_file (
    id int8 not null,
    comment text,
    pattern varchar(255),
    invalid_folder_name varchar(255),
    mandatory boolean,
    media_type varchar(255),
    meta_product_id int8,
    primary key (id)
);
create table t_acquisition_meta_product (
    id int8 not null,
    checksumAlgorithm varchar(16),
    cleanOriginalFile boolean,
    ingest_chain varchar(255),
    label varchar(64) not null,
    primary key (id)
);
create table t_acquisition_scan_directory (
    id int8 not null,
    scan_directory varchar(255),
    scan_directory_id int8,
    primary key (id)
);
create index idx_acq_meta_product_label on t_acquisition_meta_product (label);
alter table
    t_acquisition_meta_product
add
    constraint uk_acq_meta_product_label unique (label);
    
-- Product & files
create table t_acquisition_product (
    id int8 not null,
    ingest_chain varchar(255),
    ip_id varchar(80),
    product_name varchar(128),
    session varchar(128),
    json_sip jsonb,
    product_state varchar(32) not null,
    sip_state varchar(32) not null,
    last_update timestamp not null,
    meta_product_id int8,
    primary key (id)
);
create table t_acquisition_file (
    id int8 not null,
    acquisition_date timestamp,
    acquisition_directory varchar(255),
    working_directory varchar(255),
    checksum varchar(255),
    checksumAlgorithm varchar(16),
    label varchar(255) not null,
    file_size int8,
    status varchar(16),
    meta_file_id int8,
    product_id int8,
    primary key (id)
);
create index idx_acq_product_name on t_acquisition_product (product_name);
create index idx_acq_ingest_chain on t_acquisition_product (ingest_chain);
create index idx_acq_product_session on t_acquisition_product (session);
alter table
    t_acquisition_product
add
    constraint uk_acq_product_name unique (product_name);
alter table
    t_acquisition_product
add
    constraint uk_acq_product_ipId unique (ip_id);
create sequence seq_chain start 1 increment 50;
create sequence seq_exec_chain start 1 increment 50;
create sequence seq_meta_file start 1 increment 50;
create sequence seq_meta_product start 1 increment 50;
create sequence seq_product start 1 increment 50;
create sequence seq_scan_dir start 1 increment 50;
alter table
    t_acquisition_chain
add
    constraint fk_checkfile_conf_id foreign key (checkfile_conf_id) references t_plugin_configuration;
alter table
    t_acquisition_chain
add
    constraint fk_generatesip_conf_id foreign key (generatesip_conf_id) references t_plugin_configuration;
alter table
    t_acquisition_chain
add
    constraint fk_metaproduct_id foreign key (meta_product_id) references t_acquisition_meta_product;
alter table
    t_acquisition_chain
add
    constraint fk_postprocesssip_conf_id foreign key (postprocesssip_conf_id) references t_plugin_configuration;
alter table
    t_acquisition_chain
add
    constraint fk_scan_conf_id foreign key (scan_conf_id) references t_plugin_configuration;
alter table
    t_acquisition_exec_chain
add
    constraint fk_acq_exec_chain_id foreign key (chain_id) references t_acquisition_chain;
alter table
    t_acquisition_file
add
    constraint fk_meta_file_id foreign key (meta_file_id) references t_acquisition_meta_file;
alter table
    t_acquisition_file
add
    constraint fk_product_id foreign key (product_id) references t_acquisition_product;
alter table
    t_acquisition_meta_file
add
    constraint fk_meta_product_id foreign key (meta_product_id) references t_acquisition_meta_product;
alter table
    t_acquisition_product
add
    constraint fk_product_id foreign key (meta_product_id) references t_acquisition_meta_product;
alter table
    t_acquisition_scan_directory
add
    constraint fk_acq_directory foreign key (scan_directory_id) references t_acquisition_meta_file;

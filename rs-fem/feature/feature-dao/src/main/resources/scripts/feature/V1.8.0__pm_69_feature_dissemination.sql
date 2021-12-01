-- Add missing index
create index idx_feature_provider_id on t_feature (provider_id);
create index idx_feature_step_registration_priority on t_feature_request (step, registration_date, priority);

-- Add dissemination infos
alter table t_feature
    add column dissemination_pending boolean;
update t_feature set dissemination_pending = false;

create table t_feature_dissemination_info
(
    id           int8         not null,
    ack_date     timestamp,
    label        varchar(128) not null,
    request_date timestamp    not null,
    feature_id   int8,
    primary key (id)
);
alter table t_feature_dissemination_info
    add constraint fk_feature_dissemination_info_feature_id foreign key (feature_id) references t_feature;
create sequence seq_feature_dissemination_info start 1 increment 50;

-- Add dissemination update request
create table t_feature_update_dissemination
(
    id              int8         not null,
    creation_date   timestamp    not null,
    recipient_label varchar(128) not null,
    update_type     int4         not null,
    feature_urn     varchar(132) not null,
    ack_required    boolean,
    primary key (id)
);

create sequence seq_feature_update_dissemination start 1 increment 50;

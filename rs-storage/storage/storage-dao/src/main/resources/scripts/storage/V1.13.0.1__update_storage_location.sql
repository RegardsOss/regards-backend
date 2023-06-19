alter table t_storage_location
    add column pending_action_remaining boolean not null default false;
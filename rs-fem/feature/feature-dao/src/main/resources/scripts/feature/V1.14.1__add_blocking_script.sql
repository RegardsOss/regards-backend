-- Add column blocking_required from recipient
alter table t_feature_update_dissemination
    add column IF NOT EXISTS blocking_required boolean default false;
-- Add column blocking from recipient
alter table t_feature_dissemination_info
    add column IF NOT EXISTS blocking boolean default false;
-- Add column acknowledged_recipient from recipient
alter table t_feature_request
    add column IF NOT EXISTS acknowledged_recipient varchar(255);
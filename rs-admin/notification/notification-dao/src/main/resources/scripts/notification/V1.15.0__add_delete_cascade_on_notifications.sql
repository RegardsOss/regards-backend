-- Recreate foreign key constraints on t_notification to use delete on cascade
alter table ta_notification_projectuser_email drop constraint if exists
    fk_notification_projectuser_email_notification_id;

alter table ta_notification_role_name drop constraint if exists
    fk_notification_role_name_notification_id;

alter table ta_notification_projectuser_email add constraint fk_notification_projectuser_email_notification_id
    foreign key (notification_id) references t_notification on delete cascade;
alter table ta_notification_role_name add constraint fk_notification_role_name_notification_id
    foreign key (notification_id) references t_notification on delete cascade;
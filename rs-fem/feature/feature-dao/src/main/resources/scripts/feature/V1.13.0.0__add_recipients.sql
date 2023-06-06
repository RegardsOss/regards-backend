create table ta_feature_notification_request_recipient_ids
(
    feature_notification_request_id int8         not null,
    recipient_id                    varchar(255) not null,
    primary key (feature_notification_request_id, recipient_id)
);
alter table ta_feature_notification_request_recipient_ids
    add constraint fk_ta_feature_notification_request_recipient_ids foreign key (feature_notification_request_id)
        references t_feature_request (id) on delete cascade;
CREATE SEQUENCE seq_notification START 1 INCREMENT 50;
CREATE SEQUENCE seq_notification_settings START 1 INCREMENT 50;
create table t_notification (id int8 not null, date timestamp, message text, sender varchar(255), status varchar(255), title varchar(255), type varchar(255), mime_type varchar(255) DEFAULT 'text/plain', primary key (id));
create table t_notification_settings (id int8 not null, days int4, frequency varchar(255), hours int4, user_email varchar(255), primary key (id));
create table ta_notification_projectuser_email (notification_id int8 not null, projectuser_email varchar(200));
create table ta_notification_role_name (notification_id int8 not null, role_name varchar(200));
alter table ta_notification_projectuser_email add constraint fk_notification_projectuser_email_notification_id foreign key (notification_id) references t_notification;
alter table ta_notification_role_name add constraint fk_notification_role_name_notification_id foreign key (notification_id) references t_notification;

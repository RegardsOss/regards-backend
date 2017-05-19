create table t_notification (id int8 not null, date timestamp, message varchar(255), sender varchar(255), status int4, title varchar(255), primary key (id));
create table t_notification_settings (id int8 not null, days int4, frequency int4, hours int4, user_id int8, primary key (id));
create table ta_notification_projectuser (notification_id int8 not null, projectuser_id int8 not null);
create table ta_notification_role (notification_id int8 not null, role_id int8 not null);
create sequence seq_notification start 1 increment 50;
create sequence seq_notification_settings start 1 increment 50;
alter table ta_notification_projectuser add constraint fk_notification_projectuser foreign key (notification_id) references t_notification;
alter table ta_notification_role add constraint fk_role_notification foreign key (role_id) references t_role;
alter table ta_notification_role add constraint fk_notification_role foreign key (notification_id) references t_notification;

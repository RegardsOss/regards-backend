/* Locks */
create table t_lock (id int8 not null, expiration_date timestamp, lock_name text not null, locking_class_name text not null, primary key (id));
alter table t_lock drop constraint uk_lock;
alter table t_lock add constraint uk_lock unique (lock_name, locking_class_name);
create sequence seq_lock start 1 increment 50;
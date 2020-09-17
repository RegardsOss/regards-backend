create table t_dump (id int8, last_dump_req_date timestamp);
alter table t_request add previous_dump_date timestamp;
delete from ta_cache_file_group_ids;
delete from t_cache_file;
ALTER TABLE t_cache_file ADD fileName varchar(256) NOT NULL;
ALTER TABLE t_cache_file ADD mime_type varchar(255) NOT NULL;

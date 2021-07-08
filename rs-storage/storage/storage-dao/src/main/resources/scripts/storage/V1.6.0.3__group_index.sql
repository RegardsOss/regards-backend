-- ADD INDEX ON T_REQUEST_GROUP TO OPTIMIZE SEARCH
create index IF NOT EXISTS idx_t_request_group_date on t_request_group (creation_date);
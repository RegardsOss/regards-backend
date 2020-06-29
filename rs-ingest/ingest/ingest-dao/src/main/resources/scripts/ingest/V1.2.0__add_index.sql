create index IF NOT EXISTS idx_aipid on t_aip (aip_id);
create index IF NOT EXISTS idx_request_type on t_request (dtype);
create index IF NOT EXISTS idx_request_state on t_request (dtype,state);
create index IF NOT EXISTS idx_request_state_session on t_request (dtype,state,session_name);
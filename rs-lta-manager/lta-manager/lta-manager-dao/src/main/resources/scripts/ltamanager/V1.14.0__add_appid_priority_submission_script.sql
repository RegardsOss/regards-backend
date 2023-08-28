ALTER TABLE t_submission_requests
    ADD COLUMN origin_request_appid varchar(128);
ALTER TABLE t_submission_requests
    ADD COLUMN origin_request_priority int8;
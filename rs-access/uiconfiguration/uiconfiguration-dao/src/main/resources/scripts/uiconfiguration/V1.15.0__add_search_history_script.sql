CREATE TABLE t_search_history
(
    id              int8            not null primary key,
    name            varchar(255)    not null unique,
    configuration   text            not null,
    account_email   varchar(255)    not null,
    module_id       int8            not null
);

CREATE sequence seq_search_history start 1 increment 50;
CREATE index idx_search_history_account_email on t_search_history (account_email);
CREATE index idx_search_history_module_id on t_search_history (module_id)

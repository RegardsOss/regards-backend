CREATE TABLE shared_lock(
                         region VARCHAR(256),
                         lock_key VARCHAR(256),
                         client_id VARCHAR(256) NULL,
                         created_date TIMESTAMP(3) NULL,
                         PRIMARY KEY (lock_key)
)
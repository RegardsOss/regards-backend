CREATE TABLE IF NOT EXISTS t_email_verification_token
(
    id int8 NOT NULL,
    expiry_date timestamp,
    origin_url text,
    request_link text,
    token varchar(255),
    account_id int8 NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_token_account_id UNIQUE(account_id),
    CONSTRAINT fk_email_verification_token FOREIGN KEY (account_id) REFERENCES t_account
);

CREATE SEQUENCE seq_email_verification_token START 1 INCREMENT 50;

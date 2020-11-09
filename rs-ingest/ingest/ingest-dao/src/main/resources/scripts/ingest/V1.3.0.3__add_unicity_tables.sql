-- Workaround tables to ensure unicity of last version on a set of SIPs or AIPs with same provider id
-- Unicity check is done here on the transaction COMMIT only not on each statement if we use an UNIQUE INDEX

-- For SIPs
ALTER TABLE t_sip
ADD CONSTRAINT unique_sip_id_provider_id UNIQUE (id, provider_id);

CREATE TABLE t_last_sip (
    id int8 not null,
    sip_id int8 not null, 
    provider_id varchar(128),
    primary key (id),
    CONSTRAINT unique_last_sip_provider_id UNIQUE (provider_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT unique_last_sip_fk FOREIGN KEY (sip_id, provider_id)
    REFERENCES t_sip (id, provider_id) DEFERRABLE INITIALLY DEFERRED
);
create sequence seq_last_sip start 1 increment 50;

-- For AIPs
ALTER TABLE t_aip
ADD CONSTRAINT unique_aip_id_provider_id UNIQUE (id, provider_id);

CREATE TABLE t_last_aip (
    id int8 not null,
    aip_id int8 not null, 
    provider_id varchar(128),
    primary key (id),
    CONSTRAINT unique_last_aip_provider_id UNIQUE (provider_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT unique_last_aip_fk FOREIGN KEY (aip_id, provider_id)
    REFERENCES t_aip (id, provider_id) DEFERRABLE INITIALLY DEFERRED
);
create sequence seq_last_aip start 1 increment 50;

-- Remove old indexes
DROP INDEX IF EXISTS idx_one_sip_last;
DROP INDEX IF EXISTS idx_one_aip_last;
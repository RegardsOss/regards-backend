CREATE TABLE IF NOT EXISTS t_storage_location (
                                            id int8 NOT NULL,
                                            last_update_date timestamp without time zone,
                                            name varchar(128),
                                            nb_ref_files int8,
                                            total_size_ko int8,
                                            nb_ref_pending int8 DEFAULT 0,
                                            pending_action_remaining boolean DEFAULT false NOT NULL,
                                            primary key (id)
);

ALTER TABLE ONLY t_storage_location
    ADD CONSTRAINT uk_t_storage_location_name UNIQUE (name);

CREATE INDEX IF NOT EXISTS idx_storage_location ON t_storage_location USING btree (name);

CREATE TABLE IF NOT EXISTS t_storage_location_conf (
                                                 id int8 NOT NULL,
                                                 allocated_size_ko int8,
                                                 name varchar(128),
                                                 storage_type varchar(255),
                                                 plugin_conf_id int8,
                                                 primary key (id)
);

ALTER TABLE ONLY t_storage_location_conf
    ADD CONSTRAINT uk_storage_loc_name UNIQUE (name);

ALTER TABLE ONLY t_storage_location_conf
    ADD CONSTRAINT uk_storage_plugin_conf UNIQUE (plugin_conf_id);

ALTER TABLE ONLY t_storage_location_conf
    ADD CONSTRAINT fk_storage_plugin_conf FOREIGN KEY (plugin_conf_id) REFERENCES t_plugin_configuration(id);

CREATE SEQUENCE IF NOT EXISTS seq_storage_location_conf START 1 INCREMENT 50;

CREATE INDEX IF NOT EXISTS idx_storage_location ON t_storage_location_conf USING btree (storage_type);
CREATE INDEX IF NOT EXISTS idx_storage_location ON t_storage_location_conf USING btree (name);
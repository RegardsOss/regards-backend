CREATE SEQUENCE IF NOT EXISTS seq_package_reference START 1 INCREMENT 50;

CREATE TABLE IF NOT EXISTS t_package_reference
(
    id INT8         NOT NULL,
    PRIMARY KEY(id)
);

CREATE SEQUENCE IF NOT EXISTS seq_file_in_building_package START 1 INCREMENT 50;

CREATE TABLE IF NOT EXISTS t_file_in_building_package
(
    id                       INT8         NOT NULL,
    storage_request_id       INT8         NOT NULL,
    storage                  VARCHAR(255) NOT NULL,
    checksum                 VARCHAR(255) NOT NULL,
    filename                 VARCHAR(255) NOT NULL,
    filesize                 INT8         NOT NULL,
    package_id               INT8,
    status                   VARCHAR(50)  NOT NULL,
    last_update_date         TIMESTAMP    NOT NULL,
    store_parent_path        VARCHAR(255) NOT NULL,
    store_parent_url         VARCHAR(255) NOT NULL,
    keep_in_cache_until_date TIMESTAMP,
    CONSTRAINT uk_file_building_storage_checksum UNIQUE (storage, checksum),
    CONSTRAINT fk_acq_file_id FOREIGN KEY (package_id) REFERENCES t_package_reference (id),
    PRIMARY KEY(id)
);

CREATE INDEX IF NOT EXISTS idx_file_in_building_package
    ON t_file_in_building_package (storage, checksum);
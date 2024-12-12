CREATE SEQUENCE IF NOT EXISTS seq_package_reference START 1 INCREMENT 50;

CREATE TABLE IF NOT EXISTS t_package_reference
(
    id INT8              NOT NULL,
    storage_subidrectory VARCHAR(255) NOT NULL,
    creation_date        TIMESTAMP NOT NULL,
    status               VARCHAR(50) NOT NULL,
    error_cause          VARCHAR(255),
    storage              VARCHAR(255) NOT NULL,
    checksum             VARCHAR(255),
    size INT8,
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
    storage_subdirectory     VARCHAR(255) NOT NULL,
    file_cache_path          VARCHAR(255) NOT NULL,
    final_archive_parent_url VARCHAR(255) NOT NULL,
    keep_in_cache_until_date TIMESTAMP,
    error_cause              VARCHAR(255),
    CONSTRAINT uk_file_building_storage_checksum UNIQUE (storage, checksum),
    CONSTRAINT fk_acq_file_id FOREIGN KEY (package_id) REFERENCES t_package_reference (id),
    PRIMARY KEY(id)
);

CREATE INDEX IF NOT EXISTS idx_package_reference_creationdate_status
    ON t_package_reference (creation_date, status);

CREATE INDEX IF NOT EXISTS idx_file_in_building_package
    ON t_file_in_building_package (storage, checksum);

CREATE INDEX IF NOT EXISTS idx_file_in_building_package_storage_subdirectory_status
    ON t_file_in_building_package (storage, storage_subdirectory, status);
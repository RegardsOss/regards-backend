ALTER TABLE t_package_reference
    ALTER COLUMN error_cause TYPE VARCHAR(32768);

ALTER TABLE t_file_in_building_package
    ALTER COLUMN error_cause TYPE VARCHAR(32768);

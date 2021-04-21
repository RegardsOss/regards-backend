-- Modify acq_file to scan directories according to their last dates
create table t_scan_dir_info (id int8 not null, scan_directory varchar(260), last_modification_date timestamp, file_info_id int, primary key(id));
create sequence seq_scan_dir_info start 1 increment 50;
alter table t_scan_dir_info add constraint fk_file_info_id foreign key (file_info_id) references t_acq_file_info;

-- fill t_scan_dir_info with previous lastModificationDate and plugin parameter directories
INSERT INTO t_scan_dir_info(id, last_modification_date, scan_directory, file_info_id)
SELECT nextval('seq_scan_dir_info'), info.lastModificationDate, trim(both '"' from json_paths.paths::text), info.id
FROM (SELECT id, json_array_elements(json_extract_path(params, 'value')) paths
     FROM t_plugin_configuration, json_array_elements(parameters::json) params
     where params->>'name'='directories') json_paths  LEFT JOIN  t_acq_file_info info
ON json_paths.id = info.scan_conf_id;

-- fill t_scan_dir_info with previous lastModificationDate and plugin parameter directoryToScan
INSERT INTO t_scan_dir_info(id, last_modification_date, scan_directory, file_info_id)
SELECT nextval('seq_scan_dir_info'), info.lastModificationDate, trim(both '"' from json_path.path::text), info.id
FROM (SELECT id, json_extract_path(params, 'value') path
     FROM t_plugin_configuration, json_array_elements(parameters::json) params
     where params->>'name'='directoryToScan') json_path  LEFT JOIN  t_acq_file_info info
ON json_path.id = info.scan_conf_id;


-- remove attribute path from plugin configuration parameters
WITH q AS (SELECT id, (parameters - (position-1)::int)::jsonb new_parameters
FROM t_plugin_configuration, jsonb_array_elements(parameters) WITH ordinality arr(item_object, position)
WHERE item_object->>'name'='directories' OR item_object->>'name'='directoryToScan')
UPDATE t_plugin_configuration
SET parameters = q.new_parameters FROM q
WHERE t_plugin_configuration.id =  q.id;

-- drop lastModificationDate
alter table t_acq_file_info drop column lastModificationDate;

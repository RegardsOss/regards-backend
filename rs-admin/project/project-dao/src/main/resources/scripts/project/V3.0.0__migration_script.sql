-- CRS (Earth by default so 'WGS_84'
ALTER TABLE t_project ADD COLUMN crs VARCHAR (32) DEFAULT 'WGS_84';


ALTER TABLE t_project ADD COLUMN pole_managed boolean DEFAULT FALSE;
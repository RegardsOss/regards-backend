-- FILE REFERENCE add column to know is the file is referenced or stored physically
ALTER TABLE t_file_reference ADD COLUMN referenced boolean DEFAULT FALSE;
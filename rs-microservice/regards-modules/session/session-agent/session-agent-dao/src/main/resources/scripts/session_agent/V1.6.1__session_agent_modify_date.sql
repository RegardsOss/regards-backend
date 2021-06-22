-- change date name
ALTER table t_step_property_update_request RENAME COLUMN date TO creation_date;

-- add new date for database registration date
ALTER table t_step_property_update_request ADD COLUMN registration_date timestamp;
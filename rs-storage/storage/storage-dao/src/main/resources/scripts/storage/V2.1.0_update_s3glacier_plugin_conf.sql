-- Update S3 glacier plugin configuration because parameters names have changed for HPSS plugin factorization
UPDATE t_plugin_configuration
    SET parameters = REGEXP_REPLACE(parameters::text, 'Glacier_Small_File', 'Small_File', 'g')::jsonb
        WHERE pluginid='S3Glacier';
UPDATE t_plugin_configuration
    SET parameters = REGEXP_REPLACE(parameters::text, 'Glacier_Workspace_Path', 'Small_File_Workspace_Path', 'g')::jsonb
        WHERE pluginid='S3Glacier';
UPDATE t_plugin_configuration
    SET parameters = REGEXP_REPLACE(parameters::text, 'Glacier_Parallel', 'Small_File_Parallel', 'g')::jsonb
        WHERE pluginid='S3Glacier';
UPDATE t_plugin_configuration
    SET parameters = REGEXP_REPLACE(parameters::text, 'Glacier_Local_Workspace_File_Lifetime_In_Hours', 'Small_File_Local_Workspace_File_Lifetime_In_Hours', 'g')::jsonb
        WHERE pluginid='S3Glacier';
-- handle process migration by appending forbidSplitInSuborders (default false) to parameters json array
UPDATE t_plugin_configuration
SET parameters = parameters || '{"name": "forbidSplitInSuborders", "type": "BOOLEAN", "value": "false", "dynamic": "false"}'::jsonb;

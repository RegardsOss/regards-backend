ALTER TABLE t_basket_dataset ADD COLUMN file_types_sizes jsonb default null;
ALTER TABLE t_basket_dataset ADD COLUMN file_types_count jsonb default null;
ALTER TABLE t_basket_dataset ALTER COLUMN files_count TYPE int8 USING files_count::int8;

ALTER TABLE t_basket_ds_item ADD COLUMN file_types_sizes jsonb default null;
ALTER TABLE t_basket_ds_item ADD COLUMN file_types_count jsonb default null;
ALTER TABLE t_basket_ds_item ALTER COLUMN files_count TYPE int8 USING files_count::int8;
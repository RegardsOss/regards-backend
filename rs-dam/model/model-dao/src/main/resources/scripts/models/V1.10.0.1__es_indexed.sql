alter table t_attribute_model add column indexed boolean not null default false;
alter table t_restriction add column indexable_fields jsonb;
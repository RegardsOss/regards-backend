-- refactor expiration_date computation
alter table t_order alter column expiration_date drop not null;
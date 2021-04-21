-- Functionality: PM61, add label on orders (unique by user, generated when missing)
alter table t_order add column label varchar(50);
update t_order set label = CONCAT('Order of ', TO_CHAR(creation_date,'YYYY/MM/DD at HH24:MI:SS.MS'));
alter table t_order alter column label set not null;
alter table t_order add constraint uk_t_order_label_owner unique (owner, label);
-- Functionality: PM61, change notification templates to use the new label (no need to re-insert new ones, as
-- they are found through Spring then pushed in database)
delete from t_template where code = 'ORDER_CREATED_TEMPLATE';
delete from t_template where code = 'ASIDE_ORDERS_NOTIFICATION_TEMPLATE';



UPDATE t_dynamic_tenant_setting
SET class_name = 'fr.cnes.regards.modules.order.service.settings.UserOrderParameters'
WHERE class_name = 'fr.cnes.regards.modules.order.domain.settings.UserOrderParameters';
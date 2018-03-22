-- Update OrderController remove order default role
UPDATE t_resources_access SET defaultrole = 'INSTANCE_ADMIN' where resource = '/user/orders/remove/{orderId}' AND microservice = 'rs-order';

-- Reset all access rights and roles
DELETE from t_metadata where user_id in (select id from t_project_user where role_id in (select id from t_role where name = 'ADMIN'));
DELETE from t_project_user where role_id in (select id from t_role where name = 'ADMIN');
DELETE from ta_resource_role; 
DELETE from t_role where name = 'ADMIN';
DELETE from t_resources_access;

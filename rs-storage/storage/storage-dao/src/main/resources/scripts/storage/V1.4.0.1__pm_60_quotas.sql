/* public user has no quota */
/* delete in case a previous deployment let some public user quota slip through with default (unlimited) value */
delete from t_user_download_quota_limits where email = 'public@regards.com';
/* force public user quota limits to none */
insert into t_user_download_quota_limits values (0, 'public@regards.com', 0, 0);

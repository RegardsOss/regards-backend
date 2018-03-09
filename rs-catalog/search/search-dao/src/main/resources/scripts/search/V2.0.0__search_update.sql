-- Update SearchController computeFilesSummary default role
UPDATE t_resources_access SET defaultrole = 'REGISTERED_USER' where resource = '/search/dataobjects/computefilessummary';
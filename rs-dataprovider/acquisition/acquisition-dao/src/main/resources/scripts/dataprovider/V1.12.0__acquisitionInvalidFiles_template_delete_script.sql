/* template must be changed, refresh (no need to re-insert new one, as it is found through Spring then pushed in
   database)
 */
DELETE
FROM t_template
WHERE code = 'ACQUISITION_INVALID_FILES_TEMPLATE';
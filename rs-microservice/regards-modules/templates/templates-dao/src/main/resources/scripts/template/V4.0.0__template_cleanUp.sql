-- Clean template data
DROP TABLE t_template_data CASCADE;
-- clean template columns
ALTER TABLE t_template DROP COLUMN subject;
ALTER TABLE t_template DROP COLUMN description;
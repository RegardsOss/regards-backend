ALTER TABLE ${flyway:defaultSchema}.t_toponyms ADD COLUMN visible boolean DEFAULT true;
ALTER TABLE ${flyway:defaultSchema}.t_toponyms ADD COLUMN creation_date timestamp DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ${flyway:defaultSchema}.t_toponyms ADD COLUMN expiration_date timestamp;
ALTER TABLE ${flyway:defaultSchema}.t_toponyms ADD COLUMN project varchar(256);
ALTER TABLE ${flyway:defaultSchema}.t_toponyms ADD COLUMN author varchar(100);

create table ${flyway:defaultSchema}.t_toponyms (bid varchar(16) not null UNIQUE, label varchar(256) NOT NULL, label_fr varchar(256) NOT NULL, copyright varchar(512), description varchar(512), geom public.geometry(Geometry,4326), primary key (bid));
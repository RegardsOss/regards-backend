create extension POSTGIS schema ${flyway:defaultSchema} ;
create extension POSTGIS_TOPOLOGY;
SET search_path = ${flyway:defaultSchema};
SELECT PostGIS_Version();
# Add new toponyms from a shapefile

ogr2ogr -f "PostgreSQL" \
-update -append \
-skipfailures \
-lco FID=id \
-lco SCHEMA=toponyms \
-nln toponyms.t_toponyms \
"PG:host=<host> user=<login> password=<password> dbname=<dbname>" \
-lco GEOMETRY_NAME=geom \
-sql "SELECT <label from shape file> AS label, <label from shape file> AS label_fr, \
      <copiright from shape file> as copyright, <unique id from shape file> AS bid from <shapeFile>"  \
-lco PRECISION=no -nlt PROMOTE_TO_MULTI \
<shapeFile>.shp

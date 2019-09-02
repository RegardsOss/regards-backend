-- Define functions that calls the postgres native operator, to overpass the JDBC issue related to question mark
CREATE OR REPLACE FUNCTION rs_jsonb_exists_all(jsonb, text[])
RETURNS bool AS
'SELECT $1 ?& $2' LANGUAGE sql IMMUTABLE;


CREATE OR REPLACE FUNCTION rs_jsonb_exists(jsonb, text)
RETURNS bool AS
'SELECT $1 ? $2' LANGUAGE sql IMMUTABLE;


CREATE OR REPLACE FUNCTION rs_jsonb_exists_any(jsonb, text[])
RETURNS bool AS
'SELECT $1 ?| $2' LANGUAGE sql IMMUTABLE;

UPDATE ${flyway:defaultSchema}.t_toponyms
SET bid = regexp_replace(bid, '\s', '_', 'g');

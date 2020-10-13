-- first lets handle versioning on SIPs
ALTER TABLE t_sip ADD COLUMN last bool;
UPDATE t_sip s1 SET last = true WHERE s1.id = (SELECT s2.id FROM t_sip s2 WHERE s2.provider_id = s1.provider_id AND s2.version = (SELECT max(s3.version) FROM t_sip s3 WHERE s3.provider_id = s1.provider_id AND s3.state <> 'DELETED') );
UPDATE t_sip s1 SET last = false WHERE s1.id IN (SELECT s2.id FROM t_sip s2 WHERE s2.provider_id = s1.provider_id AND s2.version <> (SELECT max(s3.version) FROM t_sip s3 WHERE s3.provider_id = s1.provider_id AND s3.state <> 'DELETED') );
UPDATE t_sip s1 SET last = false WHERE s1.state = 'DELETED';
ALTER TABLE t_sip ALTER COLUMN last SET  NOT NULL;

-- now lets handle versioning on AIPs
ALTER TABLE t_aip ADD COLUMN last bool;
UPDATE t_aip a1 SET last = true WHERE a1.id = (SELECT a2.id FROM t_aip a2 WHERE a2.provider_id = a1.provider_id AND a2.version = (SELECT max(a3.version) FROM t_aip a3 WHERE a3.provider_id = a1.provider_id AND a3.state <> 'DELETED') );
UPDATE t_aip a1 SET last = false WHERE a1.id IN (SELECT a2.id FROM t_aip a2 WHERE a2.provider_id = a1.provider_id AND a2.version <> (SELECT max(a3.version) FROM t_aip a3 WHERE a3.provider_id = a1.provider_id AND a3.state <> 'DELETED') );
UPDATE t_aip a1 SET last = false WHERE a1.state = 'DELETED';
ALTER TABLE t_aip ALTER COLUMN last SET  NOT NULL;
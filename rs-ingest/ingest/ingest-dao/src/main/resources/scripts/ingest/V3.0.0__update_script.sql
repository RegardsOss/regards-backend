-- REGARDS identifier refactoring
DROP INDEX idx_sip_id;
DROP INDEX idx_aip_id;
ALTER TABLE t_sip DROP CONSTRAINT uk_sip_ipId;

alter table t_sip rename column sipId to providerId;
alter table t_sip rename column ipId to sipId;
alter table t_aip rename column ipId to aipId;

-- Recreate indexes
CREATE
	INDEX idx_sip_id ON
	t_sip(
		providerId,
		sipId,
		checksum
	);
	
CREATE
	INDEX idx_aip_id ON
	t_aip(
		id,
		aipId,
		sip_id
	);
	
-- Recreate constraint
ALTER TABLE
	t_sip ADD CONSTRAINT uk_sip_sipId UNIQUE(sipId);
		
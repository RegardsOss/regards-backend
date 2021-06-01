/* CREATE new table for owner file reference association */
create table ta_file_reference_owner (file_ref_id int8 not null, owner varchar(255));
alter table ta_file_reference_owner add constraint fk_file_ref_owner foreign key (file_ref_id) references t_file_reference;
create index idx_ta_file_ref_owner_owner on ta_file_reference_owner (owner);
create index idx_ta_file_ref_owner_file_id on ta_file_reference_owner (file_ref_id);

/* REPLACE owners column to owners assoccation table for existing file references */
DO $$
DECLARE
  sql text;
  rec record;
  rec2 record;
BEGIN
      sql:='SELECT id,owners from t_file_reference';
      FOR rec in EXECUTE sql
      LOOP
        FOR rec2 in (select jsonb_array_elements_text(rec.owners) as owner_)
        LOOP
          INSERT INTO ta_file_reference_owner(file_ref_id, owner) VALUES(rec.id, rec2.owner_);
        END LOOP;
      END LOOP;
END;
$$ LANGUAGE plpgsql;

/* DROP unused previous owner column */
alter table t_file_reference drop column owners;

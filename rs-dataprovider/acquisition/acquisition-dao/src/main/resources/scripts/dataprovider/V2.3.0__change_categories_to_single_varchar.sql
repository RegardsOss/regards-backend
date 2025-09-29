-- Change the categories in the Processing Chain table from a list in jsonb format to a single varchar field
-- (only one category is now allowed).
-- Migrate data from the old categories column to the new category column:
-- The first category of the old categories column is set as the value of the new category column

-- create the new category column
ALTER TABLE t_acq_processing_chain
    ADD COLUMN IF NOT EXISTS category varchar(128);

DO
$$
    DECLARE
        column_exists BOOLEAN;
    BEGIN
        -- To prevent errors if this script is executed several times, we check if the categories column exists

        -- Check if the 'categories' column exists in the table 't_acq_processing_chain'
        SELECT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 't_acq_processing_chain'
                         AND column_name = 'categories')
        INTO column_exists;

        IF column_exists THEN
            -- Copy the first element of the categories column to the new category column
            UPDATE t_acq_processing_chain
            SET category = categories ->> 0
            WHERE jsonb_typeof(categories) = 'array'
              AND jsonb_array_length(categories) > 0;
        ELSE
            RAISE NOTICE 'Column "categories" does not exist in table "t_acq_processing_chain". No update performed.';
        END IF;
    END
$$;

-- Delete old categories columns
ALTER TABLE t_acq_processing_chain
    DROP COLUMN IF EXISTS categories;
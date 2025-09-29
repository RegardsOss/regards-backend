-- Change the categories in AIP and SIP tables from a list in jsonb format to a single varchar field
-- (only one category is now allowed).
-- Migrate data from the old categories column to the new category column:
-- The first category of the old categories column is set as the value of the new category column

-- Create the new category columns
ALTER TABLE t_aip
    ADD COLUMN IF NOT EXISTS category varchar(128);

ALTER TABLE t_sip
    ADD COLUMN IF NOT EXISTS category varchar(128);

-- Delete old index
DROP INDEX IF EXISTS idx_aip_categories;

DO
$$
    DECLARE
        aip_column_exists BOOLEAN;
        sip_column_exists BOOLEAN;
    BEGIN
        -- To prevent errors if this script is executed several times, we check if the categories column exists

        -- Check if the 'categories' column exists in the table 't_aip'
        SELECT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 't_aip'
                         AND column_name = 'categories')
        INTO aip_column_exists;
        -- Check if the 'categories' column exists in the table 't_sip'
        SELECT EXISTS (SELECT 1
                       FROM information_schema.columns
                       WHERE table_name = 't_sip'
                         AND column_name = 'categories')
        INTO sip_column_exists;

        IF aip_column_exists THEN
            -- Copy the first element of the categories column to the new category column
            LOOP
                WITH cte AS (
                    SELECT id
                    FROM t_aip
                    WHERE category IS NULL AND jsonb_typeof(categories) = 'array' AND jsonb_array_length(categories) > 0
                    LIMIT 2000
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE t_aip aip
                SET category = categories ->> 0
                FROM cte
                WHERE aip.id = cte.id;

                EXIT WHEN NOT FOUND;
                COMMIT;
            END LOOP;
        ELSE
            RAISE NOTICE 'Column "categories" does not exist in table "t_aip". No update performed.';
        END IF;

        IF sip_column_exists THEN
            -- Copy the first element of the categories column to the new category column
            LOOP
                WITH cte AS (
                    SELECT id
                    FROM t_sip
                    WHERE category IS NULL AND jsonb_typeof(categories) = 'array' AND jsonb_array_length(categories) > 0
                    LIMIT 2000
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE t_sip sip
                SET category = categories ->> 0
                FROM cte
                WHERE sip.id = cte.id;

                EXIT WHEN NOT FOUND;
                COMMIT;
            END LOOP;
        ELSE
            RAISE NOTICE 'Column "categories" does not exist in table "t_sip". No update performed.';
        END IF;
    END
$$;

-- Create index for the new column (t_aip only)
CREATE INDEX IF NOT EXISTS idx_aip_category on t_aip (category);

-- Delete old categories columns
ALTER TABLE t_aip
    DROP COLUMN IF EXISTS categories;

ALTER TABLE t_sip
    DROP COLUMN IF EXISTS categories;
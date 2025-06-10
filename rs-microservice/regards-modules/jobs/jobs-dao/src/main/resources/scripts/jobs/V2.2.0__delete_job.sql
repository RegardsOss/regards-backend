-- Define functions that remove job from the database ignoring foreign key constraints
CREATE OR REPLACE PROCEDURE deleteExpiredJobs(
    expirationDate TIMESTAMP WITH TIME ZONE,
    statuses TEXT[]
)
AS $$
BEGIN
    BEGIN
        DELETE FROM t_job_info WHERE locked = false AND expire_date <= expirationDate AND status = ANY(statuses::VARCHAR[]);
    EXCEPTION
        WHEN foreign_key_violation THEN
            NULL;
    END;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE deleteFinishedJobs(
    stopDate TIMESTAMP WITH TIME ZONE,
    statuses TEXT[]
)
AS $$
BEGIN
    BEGIN
        DELETE FROM t_job_info WHERE locked = false AND stop_date <= stopDate AND status = ANY(statuses::VARCHAR[]);
    EXCEPTION
            WHEN foreign_key_violation THEN
                NULL;
    END;
END;
$$ LANGUAGE plpgsql;

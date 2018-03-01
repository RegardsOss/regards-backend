-- Add project connection state
ALTER TABLE
	t_project_connection ADD COLUMN state VARCHAR(50);

-- Update project connection states
UPDATE
	t_project_connection
SET
	state = 'DISABLED';

UPDATE
	t_project_connection
SET
	state = 'ENABLED'
WHERE
	enabled = TRUE;

-- Add column constraint
ALTER TABLE
	t_project_connection ALTER COLUMN state
SET
	NOT NULL;

-- Remove column
ALTER TABLE
	t_project_connection DROP
		COLUMN enabled;

-- Add project connection error cause
ALTER TABLE
	t_project_connection ADD COLUMN cause VARCHAR(255);
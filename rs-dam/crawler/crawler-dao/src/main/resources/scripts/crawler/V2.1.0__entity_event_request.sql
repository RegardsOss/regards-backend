CREATE SEQUENCE IF NOT EXISTS entity_event_request START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS t_entity_event_request
(
    id     BIGINT PRIMARY KEY DEFAULT NEXTVAL('entity_event_request'),
    urn    VARCHAR(255) NOT NULL,
    status VARCHAR(255),
    userToNotify VARCHAR(255),
    roleToNotify VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_entity_event_request_status_urn
    ON t_entity_event_request (status, urn);
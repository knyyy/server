-- Populate the initially empty andwellness database.
-- This SQL is intended to be run after andwellness-ddl.sql.

INSERT INTO campaign (name, label) VALUES ('CHIPTS', 'CHIPTS');

INSERT INTO user_role (label) VALUES ('admin'), ('participant'), ('researcher');

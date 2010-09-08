-- Populate the initially empty andwellness database.
-- This SQL is intended to be run after andwellness-ddl.sql.

INSERT INTO campaign (name, label) VALUES ('CHIPTS', 'CHIPTS ');

INSERT INTO user_role (label) VALUES ('admin'), ('participant'), ('researcher');

INSERT INTO prompt_type (type) VALUES
  ("timestamp"), ("number"), ("hours_before_now"), ("text"), ("multi_choice"), ("single_choice"), ("single_choice_custom"),
  ("multi_choice_custom"), ("photo");
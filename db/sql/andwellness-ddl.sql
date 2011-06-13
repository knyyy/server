-- MySQL DDL statements for the AndWellness database

CREATE DATABASE andwellness CHARACTER SET utf8 COLLATE utf8_general_ci;
USE andwellness;

-- --------------------------------------------------------------------
-- The class concept comes from Mobilize, but it can be used for any
-- taxonomical grouping of users. 
-- --------------------------------------------------------------------
CREATE TABLE class (
  id int unsigned NOT NULL auto_increment,
  urn varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  description text,
  PRIMARY KEY (id),
  UNIQUE (urn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Lookup table for the running states of a campaign.
-- --------------------------------------------------------------------
CREATE TABLE campaign_running_state (
  id int unsigned NOT NULL auto_increment,
  running_state varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (running_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Lookup table for the privacy states of a campaign.
-- --------------------------------------------------------------------
CREATE TABLE campaign_privacy_state (
  id int unsigned NOT NULL auto_increment,
  privacy_state varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (privacy_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- A campaign and its associated XML configuration.
-- --------------------------------------------------------------------
CREATE TABLE campaign (
  id int unsigned NOT NULL auto_increment,
  urn varchar(255) NOT NULL,
  name varchar(255) NOT NULL,
  description text,
  xml mediumtext NOT NULL,
  running_state_id int unsigned NOT NULL,
  privacy_state_id int unsigned NOT NULL,
  creation_timestamp datetime NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (urn),
  CONSTRAINT FOREIGN KEY (running_state_id) REFERENCES campaign_running_state (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (privacy_state_id) REFERENCES campaign_privacy_state (id) ON DELETE CASCADE ON UPDATE CASCADE
-- create an index on name?
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Bind campaigns to classes.
-- --------------------------------------------------------------------
CREATE TABLE campaign_class (
  id int unsigned NOT NULL auto_increment,
  campaign_id int unsigned NOT NULL,
  class_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (campaign_id, class_id),
  CONSTRAINT FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (class_id) REFERENCES class (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- -----------------------------------------------------------------------
-- System users.
-- -----------------------------------------------------------------------
CREATE TABLE user (
  id int unsigned NOT NULL auto_increment,
  username varchar(15) NOT NULL,
  password varchar(100) NOT NULL,
  enabled bit NOT NULL,
  new_account bit NOT NULL,
  campaign_creation_privilege bit NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ---------------------------------------------------------------------
-- Due to IRB standards, we store personally identifying information
-- separately from the user's login credentials. ** This table is currently
-- unused, but it is kept around in order to avoid changing the command
-- line registration process. **
-- ---------------------------------------------------------------------
CREATE TABLE user_personal (
  id int unsigned NOT NULL auto_increment,
  user_id int unsigned NOT NULL,
  first_name varchar(255) NOT NULL,
  last_name varchar(255) NOT NULL,
  organization varchar(255) NOT NULL,
  personal_id varchar(255) NOT NULL,  -- this is e.g., the Mobilize student's student id
  email_address varchar(320),
  json_data text,
  PRIMARY KEY (id),
  UNIQUE (user_id),
  UNIQUE (first_name, last_name, organization, personal_id), 
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- User role lookup table.
-- --------------------------------------------------------------------
CREATE TABLE user_role (
  id tinyint unsigned NOT NULL auto_increment,
  role varchar(50) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Bind users to roles and campaigns. A user can have a different role
-- for each campaign they belong to.
-- --------------------------------------------------------------------
CREATE TABLE user_role_campaign (
  id int unsigned NOT NULL auto_increment,
  user_id int unsigned NOT NULL,
  campaign_id int unsigned NOT NULL,
  user_role_id tinyint unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (user_id, campaign_id, user_role_id),
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (user_role_id) REFERENCES user_role (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Roles for a user within a class.
-- --------------------------------------------------------------------
CREATE TABLE user_class_role (
  id int unsigned NOT NULL auto_increment,
  role varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Bind users to classes. 
-- --------------------------------------------------------------------
CREATE TABLE user_class (
  id int unsigned NOT NULL auto_increment,
  user_id int unsigned NOT NULL,
  class_id int unsigned NOT NULL,
  user_class_role_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (user_id, class_id),
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (class_id) REFERENCES class (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (user_class_role_id) REFERENCES user_class_role (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Add a default role to campaign class relationships.
-- --------------------------------------------------------------------
CREATE TABLE campaign_class_default_role (
  id int unsigned NOT NULL auto_increment,
  campaign_class_id int unsigned NOT NULL,
  user_class_role_id int unsigned NOT NULL,
  user_role_id tinyint unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (campaign_class_id, user_class_role_id, user_role_id),
  CONSTRAINT FOREIGN KEY (campaign_class_id) REFERENCES campaign_class (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (user_class_role_id) REFERENCES user_class_role (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (user_role_id) REFERENCES user_role (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Survey response privacy states.
-- --------------------------------------------------------------------
CREATE TABLE survey_response_privacy_state (
  id int unsigned NOT NULL auto_increment,
  privacy_state varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (privacy_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Stores survey responses for a user in a campaign 
-- --------------------------------------------------------------------
CREATE TABLE survey_response (
  id int unsigned NOT NULL auto_increment,
  user_id int unsigned NOT NULL,
  campaign_id int unsigned NOT NULL,
  client varchar(250) NOT NULL,
  msg_timestamp datetime NOT NULL,
  epoch_millis bigint unsigned NOT NULL, 
  phone_timezone varchar(32) NOT NULL,
  survey_id varchar(250) NOT NULL,    -- a survey id as defined in a campaign at the XPath //surveyId
  survey text NOT NULL,               -- the max length for text is 21843 UTF-8 chars
  launch_context text,                -- trigger and other data
  location_status tinytext NOT NULL,  -- one of: unavailable, valid, stale, inaccurate 
  location text,                      -- JSON location data: longitude, latitude, accuracy, provider
  upload_timestamp datetime NOT NULL, -- the upload time based on the server time and timezone  
  audit_timestamp timestamp default current_timestamp on update current_timestamp,
  privacy_state_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  INDEX (user_id, campaign_id),
  INDEX (user_id, upload_timestamp),
  UNIQUE (campaign_id, user_id, survey_id, epoch_millis), -- handle duplicate survey uploads
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,    
  CONSTRAINT FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (privacy_state_id) REFERENCES survey_response_privacy_state (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Stores individual prompt responses for a user in a campaign. Both
-- the entire survey response and each prompt response in for a survey
-- are stored.
-- --------------------------------------------------------------------
CREATE TABLE prompt_response (
  id int unsigned NOT NULL auto_increment,
  survey_response_id int unsigned NOT NULL,
  prompt_id varchar(250) NOT NULL,  -- a prompt id as defined in a configuration at the XPath //promptId
  prompt_type varchar(250) NOT NULL, -- a prompt type as defined in a configuration at the XPath //promptType
  repeatable_set_id varchar(250), -- a repeatable set id as defined in a configuration at the XPath //repeatableSetId
  repeatable_set_iteration tinyint unsigned,
  response text NOT NULL,   -- the data format is defined by the prompt type: a string or a JSON string
  audit_timestamp timestamp default current_timestamp on update current_timestamp,
  PRIMARY KEY (id),
  INDEX (survey_response_id),
  INDEX (prompt_id),
  -- uniqueness of survey uploads is handled by the survey_response table
  
  CONSTRAINT FOREIGN KEY (survey_response_id) REFERENCES survey_response (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Points a UUID to a URL of a media resource (such as an image). The  
-- UUID is an implicit link into the prompt_response table. The privacy
-- for images is handled by the link up to the survey_response via
-- prompt_response.
-- --------------------------------------------------------------------
CREATE TABLE url_based_resource (
    id int unsigned NOT NULL auto_increment,
    user_id int unsigned NOT NULL,
    client varchar(250) NOT NULL,
    uuid char (36) NOT NULL, -- joined with prompt_response.response to retrieve survey context for an item
    url text,
    audit_timestamp timestamp default current_timestamp on update current_timestamp,
    
    UNIQUE (uuid), -- disallow duplicates and index on UUID
    PRIMARY KEY (id),
    CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Mobility privacy states.
-- --------------------------------------------------------------------
CREATE TABLE mobility_privacy_state (
  id int unsigned NOT NULL auto_increment,
  privacy_state varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (privacy_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- High-frequency "mode only" mobility data. Mobility data is *not*
-- linked to a campaign.
-- --------------------------------------------------------------------
CREATE TABLE mobility_mode_only (
  id int unsigned NOT NULL auto_increment,
  user_id int unsigned NOT NULL,
  client tinytext NOT NULL,
  msg_timestamp datetime NOT NULL,
  epoch_millis bigint unsigned NOT NULL,
  phone_timezone varchar(32) NOT NULL,
  location_status tinytext NOT NULL,
  location text,
  mode varchar(30) NOT NULL,
  upload_timestamp datetime NOT NULL, -- the upload time based on the server time and timezone
  audit_timestamp timestamp default current_timestamp on update current_timestamp,
  privacy_state_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  INDEX (user_id, msg_timestamp),
  UNIQUE (user_id, epoch_millis), -- enforce no-duplicates rule at the table level
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (privacy_state_id) REFERENCES mobility_privacy_state (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- High-frequency "mode + sensor data" mobility data. Mobility data is 
-- *not* linked to a campaign.
-- --------------------------------------------------------------------
CREATE TABLE mobility_extended (
  id int unsigned NOT NULL auto_increment,
  user_id int unsigned NOT NULL,
  client tinytext NOT NULL,
  msg_timestamp datetime NOT NULL,
  epoch_millis bigint unsigned NOT NULL,
  phone_timezone varchar(32) NOT NULL,
  location_status tinytext NOT NULL,
  location text,
  sensor_data text NOT NULL,
  features text NOT NULL,
  classifier_version tinytext NOT NULL, 
  mode varchar(30) NOT NULL,
  upload_timestamp datetime NOT NULL, -- the upload time based on the server time and timezone
  audit_timestamp timestamp default current_timestamp on update current_timestamp,
  privacy_state_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  INDEX (user_id, msg_timestamp),
  UNIQUE INDEX (user_id, epoch_millis),
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (privacy_state_id) REFERENCES mobility_privacy_state (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Preferences table to hold key-value pairs of items that need to be
-- stored but we don't want to store in configuration files.
-- --------------------------------------------------------------------
CREATE TABLE preference (
  p_key varchar(50) NOT NULL,
  p_value varchar(255) NOT NULL,
  UNIQUE (p_key, p_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Lookup table for the privacy states of a document.
-- --------------------------------------------------------------------
CREATE TABLE document_privacy_state (
  id int unsigned NOT NULL auto_increment,
  privacy_state varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (privacy_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Lookup table for roles for a document.
-- --------------------------------------------------------------------
CREATE TABLE document_role (
  id int unsigned NOT NULL auto_increment,
  role varchar(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Documents associated with a campaign.
-- --------------------------------------------------------------------
CREATE TABLE document (
  id int unsigned NOT NULL auto_increment,
  uuid char(36) NOT NULL,
  name varchar(255) NOT NULL,
  description text,
  extension varchar(50),
  url text NOT NULL,
  size int unsigned NOT NULL,
  privacy_state_id int unsigned NOT NULL,
  last_modified_timestamp timestamp default current_timestamp on update current_timestamp,
  PRIMARY KEY (id),
  UNIQUE (uuid),
  CONSTRAINT FOREIGN KEY (privacy_state_id) REFERENCES document_privacy_state (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Link of documents to classes.
-- --------------------------------------------------------------------
CREATE TABLE document_class_role (
  id int unsigned NOT NULL auto_increment,
  document_id int unsigned NOT NULL,
  class_id int unsigned NOT NULL,
  document_role_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (document_id, class_id),
  CONSTRAINT FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (class_id) REFERENCES class (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Link of documents to campaigns.
-- --------------------------------------------------------------------
CREATE TABLE document_campaign_role (
  id int unsigned NOT NULL auto_increment,
  document_id int unsigned NOT NULL,
  campaign_id int unsigned NOT NULL,
  document_role_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (document_id, campaign_id),
  CONSTRAINT FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (campaign_id) REFERENCES campaign (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Link of documents to users.
-- --------------------------------------------------------------------
CREATE TABLE document_user_role (
  id int unsigned NOT NULL auto_increment,
  document_id int unsigned NOT NULL,
  user_id int unsigned NOT NULL,
  document_role_id int unsigned NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (document_id, user_id),
  CONSTRAINT FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------
-- Audit trail for who created which document
-- --------------------------------------------------------------------
CREATE TABLE document_user_creator (
  id int unsigned NOT NULL auto_increment,
  document_id int unsigned NOT NULL,
  user_id int unsigned NOT NULL,
  creation_timestamp datetime NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (document_id),
  CONSTRAINT FOREIGN KEY (document_id) REFERENCES document (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
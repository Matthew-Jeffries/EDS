USE eds;

DROP TABLE IF EXISTS patient_link;
DROP TABLE IF EXISTS patient_link_history;
DROP TABLE IF EXISTS patient_link_person;
DROP TABLE IF EXISTS patient_search_local_identifier;
DROP TABLE IF EXISTS patient_search_episode;
DROP TABLE IF EXISTS patient_search;
DROP TABLE IF EXISTS patient_address_uprn;
DROP TABLE IF EXISTS patient_search_address;

CREATE TABLE patient_link
(
  patient_id character(36) NOT NULL,
  service_id character(36) NOT NULL,
  person_id character(36) NOT NULL,
  CONSTRAINT pk_patient_link PRIMARY KEY (patient_id)
);

CREATE INDEX ix_person_id
  ON patient_link (person_id);


CREATE TABLE patient_link_history
(
  patient_id character(36) NOT NULL,
  service_id character(36) NOT NULL,
  updated timestamp NOT NULL,
  new_person_id character(36) NOT NULL,
  previous_person_id character(36),
  CONSTRAINT pk_patient_link_history PRIMARY KEY (patient_id, updated)
);

CREATE INDEX ix_updated
  ON patient_link_history (updated);

create index ix_new_person_id on eds.patient_link_history (new_person_id);

CREATE TABLE patient_link_person
(
  person_id character(36) NOT NULL,
  nhs_number character(10) NOT NULL,
  CONSTRAINT pk_patient_link_person PRIMARY KEY (person_id)
);

CREATE UNIQUE INDEX ix_nhs_number
  ON patient_link_person (nhs_number);




CREATE TABLE patient_search
(
	service_id char(36) NOT NULL,
	nhs_number varchar(10),
	forenames varchar(500),
	surname varchar(500),
	date_of_birth date,
	date_of_death date,
	address_line_1 VARCHAR(255),
	address_line_2 VARCHAR(255),
	address_line_3 VARCHAR(255),
	city VARCHAR(255),
	district VARCHAR(255),
	postcode varchar(8),
	gender varchar(7),
	patient_id char(36) NOT NULL,
	last_updated timestamp NOT NULL,
	registered_practice_ods_code VARCHAR(50),
	dt_deleted datetime,
	ods_code varchar(50),
	organisation_name VARCHAR(255),
	organisation_type_code varchar(10),
	CONSTRAINT pk_patient_search PRIMARY KEY (service_id, patient_id)
);

CREATE INDEX ix_patient
  ON patient_search (patient_id);

-- duplicate of primary key (clusterd index) so removed
/*CREATE INDEX ix_service_patient
  ON patient_search (service_id, patient_id);*/

CREATE INDEX ix_service_date_of_birth
  ON patient_search (service_id, date_of_birth, dt_deleted);

-- swap index to be NHS Number first, since that's more selective than a long list of service IDs
/*CREATE INDEX ix_service_nhs_number
  ON patient_search (service_id, nhs_number);*/

CREATE INDEX ix_service_nhs_number_2
  ON patient_search (nhs_number, service_id, dt_deleted);

CREATE INDEX ix_service_surname_forenames
  ON patient_search (service_id, surname, forenames, dt_deleted);



CREATE TABLE patient_search_episode
(
	service_id char(36) NOT NULL,
	patient_id char(36) NOT NULL,
	episode_id char(36) NOT NULL,
	registration_start date,
	registration_end date,
	care_mananger VARCHAR(255),
	organisation_name VARCHAR(255),
	organisation_type_code varchar(10),
	registration_type_code varchar(10),
	last_updated timestamp NOT NULL,
	registration_status_code varchar(10),
	dt_deleted datetime,
	ods_code varchar(50),
	CONSTRAINT pk_patient_search_episode PRIMARY KEY (service_id, patient_id, episode_id)
);

-- unique index required so patient merges trigger a change in patient_id
CREATE UNIQUE INDEX uix_patient_search_episode_id
  ON patient_search_episode (episode_id);

CREATE TABLE patient_search_local_identifier
(
	service_id char(36) NOT NULL,
	local_id varchar(255),
	local_id_system varchar(255),
	patient_id char(36) NOT NULL,
	last_updated timestamp NOT NULL,
	dt_deleted datetime,
	CONSTRAINT pk_patient_search_local_identifier PRIMARY KEY (service_id, patient_id, local_id_system, local_id)
);

-- index so patient search by local ID works in timely fashion
CREATE INDEX ix_patient_search_local_identifier_id_service_patient
  ON patient_search_local_identifier (local_id, service_id, patient_id, dt_deleted);

create table patient_address_uprn (
	service_id char(36) not null,
    patient_id char(36) not null,
    uprn bigint,
    qualifier varchar(50),
    abp_address varchar(1024),
    `algorithm` varchar(255),
    `match` varchar(255),
    no_address boolean,
    invalid_address boolean,
    missing_postcode boolean,
    invalid_postcode boolean,
    CONSTRAINT pk_patient_search PRIMARY KEY (service_id, patient_id)
);

/*
-- not sure about this table yet
create table patient_search_address (
	service_id char(36) NOT NULL,
	patient_id char(36) NOT NULL,
    ordinal tinyint NOT NULL,
	`use` varchar(10),
    start_date date,
    end_date date,
    address_line_1 varchar(255),
    address_line_2 varchar(255),
    address_line_3 varchar(255),
    address_line_4 varchar(255),
    city varchar(255),
    district varchar(255),
    postcode varchar(10),
    last_updated timestamp NOT NULL,
    uprn_results JSON,
    uprn_last_updated timestamp,
	CONSTRAINT pk_patient_search_episode PRIMARY KEY (service_id, patient_id, ordinal)
);
*/
use publisher_common;

DROP TABLE IF EXISTS emis_csv_code_map;
DROP TABLE IF EXISTS emis_admin_resource_cache;

CREATE TABLE emis_csv_code_map (
	medication boolean,
	code_id bigint,
	code_type varchar(250),
	codeable_concept text,
	read_term varchar(500),
	read_code varchar(250),
	snomed_concept_id BIGINT,
	snomed_description_id BIGINT,
	snomed_term varchar(500),
	national_code varchar(250),
	national_code_category varchar(500),
	national_code_description varchar(500),
	parent_code_id bigint,
    CONSTRAINT pk_emis_csv_code_map PRIMARY KEY (medication, code_id)
);

CREATE TABLE emis_admin_resource_cache (
	data_sharing_agreement_guid varchar(36),
	emis_guid varchar(50), -- emis GUIDs are padded to longer than usual
	resource_type varchar(50),
	resource_data text,
    CONSTRAINT pk_emis_admin_resource_cache PRIMARY KEY (data_sharing_agreement_guid, emis_guid, resource_type)
);



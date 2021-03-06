use staging_barts;

drop table if exists condition_cds;
drop table if exists condition_cds_latest;
drop table if exists condition_cds_count;
drop table if exists condition_cds_count_latest;
drop table if exists condition_cds_tail;
drop table if exists condition_cds_tail_latest;
drop table if exists condition_diagnosis;
drop table if exists condition_diagnosis_latest;
drop table if exists condition_DIAGN;
drop table if exists condition_DIAGN_latest;
drop table if exists condition_problem;
drop table if exists condition_problem_latest;

drop table if exists condition_target;
drop table if exists condition_target_latest;

-- records from sus inpatient, outpatient and emergency files are written to this table, with a record PER diagnosis
-- NOTE: there is no diagnosis_date so cds_activity_date is used
create table condition_cds
(
    exchange_id                    char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                    datetime     NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                bigint       NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date              datetime     NOT NULL COMMENT 'Date common to all sus files. NOTE: used for diagnosis date',
    sus_record_type                varchar(10)  NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier          varchar(50)  NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type                int          NOT NULL COMMENT 'from CDSUpdateType',
    mrn                            varchar(10)  NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                     varchar(10)  NOT NULL COMMENT 'from NHSNumber',
    withheld                       bool     COMMENT 'True if id is withheld',
    date_of_birth                  date     COMMENT 'from PersonBirthDate',
    consultant_code                varchar(20) NOT NULL COMMENT 'GMC number of consultant, from ConsultantCode',
    diagnosis_icd_code             varchar(7)   NOT NULL COMMENT 'icd-10 code PrimaryDiagnosisICD, SecondaryDiagnosisICD etc.',
    diagnosis_seq_nbr              smallint     NOT NULL COMMENT 'number of this diagnosis in the CDS record',
    primary_diagnosis_icd_code     varchar(7) COMMENT 'ics-10 code from PrimaryDiagnosisICD - will be null if this record is for the primary diagnosis',
    lookup_diagnosis_icd_term      varchar(255) NOT NULL COMMENT 'term for above icd-10 code, looked up using TRUD',
    lookup_person_id               int COMMENT 'person ID looked up using NHS number, DoB and MRN',
    lookup_consultant_personnel_id int COMMENT 'personnel ID looked up using consultant code',
    audit_json                     mediumtext   null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_cds PRIMARY KEY (exchange_id, cds_unique_identifier, sus_record_type, diagnosis_seq_nbr)
);

CREATE INDEX ix_condition_cds_checksum_helper on condition_cds (cds_unique_identifier, sus_record_type, diagnosis_seq_nbr, dt_received);

create table condition_cds_latest
(
    exchange_id                    char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                    datetime     NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                bigint       NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date              datetime     NOT NULL COMMENT 'Date common to all sus files. NOTE: used for diagnosis date',
    sus_record_type                varchar(10)  NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier          varchar(50)  NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type                int          NOT NULL COMMENT 'from CDSUpdateType',
    mrn                            varchar(10)  NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                     varchar(10)  NOT NULL COMMENT 'from NHSNumber',
    withheld                       bool          COMMENT 'True if id is withheld',
    date_of_birth                  date COMMENT 'from PersonBirthDate',
    consultant_code                varchar(20) COMMENT 'GMC number of consultant, from ConsultantCode',
    diagnosis_icd_code             varchar(7)   NOT NULL COMMENT 'icd-10 code PrimaryDiagnosisICD, SecondaryDiagnosisICD etc.',
    diagnosis_seq_nbr              smallint     NOT NULL COMMENT 'number of this diagnosis in the CDS record',
    primary_diagnosis_icd_code     varchar(7) COMMENT 'ics-10 code from PrimaryDiagnosisICD - will be null if this record is for the primary diagnosis',
    lookup_diagnosis_icd_term      varchar(255) NOT NULL COMMENT 'term for above icd-10 code, looked up using TRUD',
    lookup_person_id               int COMMENT 'person ID looked up using NHS number, DoB and MRN',
    lookup_consultant_personnel_id int COMMENT 'personnel ID looked up using consultant code',
    audit_json                     mediumtext   null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_cds_latest PRIMARY KEY (cds_unique_identifier, sus_record_type, diagnosis_seq_nbr)
);

CREATE INDEX ix_condition_cds_latest_join_helper on condition_cds_latest (exchange_id, cds_unique_identifier, sus_record_type, diagnosis_seq_nbr);

create table condition_cds_count
(
    exchange_id                    char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                    datetime     NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                int          NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    sus_record_type                varchar(10)  NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier          varchar(50)  NOT NULL COMMENT 'from CDSUniqueIdentifier',
    condition_count                int          NOT NULL COMMENT 'number of conditions in this CDS record',
    audit_json                     mediumtext COMMENT 'used to link back to the source file',
    CONSTRAINT pk_condition_cds_count PRIMARY KEY (exchange_id, cds_unique_identifier, sus_record_type)
);

-- index to make it easier to find last checksum for a record
CREATE INDEX ix_condition_cds_count_checksum_helper on condition_cds_count (cds_unique_identifier, sus_record_type, dt_received);

create table condition_cds_count_latest
(
    exchange_id                    char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                    datetime     NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                int          NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    sus_record_type                varchar(10)  NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier          varchar(50)  NOT NULL COMMENT 'from CDSUniqueIdentifier',
    condition_count                int          NOT NULL COMMENT 'number of conditions in this CDS record',
    audit_json                     mediumtext COMMENT 'used to link back to the source file',
    CONSTRAINT pk_condition_cds_count PRIMARY KEY (cds_unique_identifier, sus_record_type)
);

-- records from sus inpatient, outpatient and emergency tail files are all written to this table with sus_record_type telling us which is which
create table condition_cds_tail
(
    exchange_id                  char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                  datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum              bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    sus_record_type              varchar(10) NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier        varchar(50),
    cds_update_type              int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                          varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                   varchar(10) NOT NULL COMMENT 'from NHSNumber',
    person_id                    int,
    encounter_id                 int,
    responsible_hcp_personnel_id int COMMENT 'from Responsible_HCP_Personal_ID',
    audit_json                   mediumtext  null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_cds_tail PRIMARY KEY (exchange_id, cds_unique_identifier, sus_record_type)
);

CREATE INDEX ix_condition_cds_tail_checksum_helper on condition_cds_tail (cds_unique_identifier, sus_record_type, dt_received);


create table condition_cds_tail_latest
(
    exchange_id                  char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                  datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum              bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    sus_record_type              varchar(10) NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier        varchar(50) NOT NULL,
    cds_update_type              int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                          varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                   varchar(10) NOT NULL COMMENT 'from NHSNumber',
    person_id                    int NOT NULL,
    encounter_id                 int NOT NULL,
    responsible_hcp_personnel_id int NOT NULL COMMENT 'from Responsible_HCP_Personal_ID',
    audit_json                   mediumtext  null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_cds_tail_latest PRIMARY KEY (cds_unique_identifier, sus_record_type)
);

-- records from the fixed-width Diagnosis file
create table condition_diagnosis
(
    exchange_id                     char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                     datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                 bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    diagnosis_id                    int         NOT NULL COMMENT 'from diagnosis_id, but standardised to remove trailing .00. Joins to condition_DIAGN.diagnosis_id',
    person_id                       int         NOT NULL COMMENT 'from person_id',
    active_ind                      bool        NOT NULL COMMENT 'whether an active record or not (deleted), from active_ind',
    mrn                             varchar(10) NOT NULL COMMENT 'from MRN',
    encounter_id                    int         NOT NULL COMMENT 'from encntr_id, but standardised to remove trailing .00',
    diag_dt_tm                      datetime    COMMENT 'from diag_dt. The date of the diagnosis',
    diag_type                       varchar(255) NOT NULL COMMENT 'text based diagnosis type',
    diag_prnsl                      varchar(255) NOT NULL COMMENT 'text based diagnosis performer',
    vocab                           varchar(50) NOT NULL COMMENT 'diagnosis code type, either SNOMED CT or UK ED Subset (Snomed)',
    diag_code                       varchar(50) NOT NULL COMMENT 'diagnosis code of type described by vocab',
    diag_term                       varchar(255) NOT NULL COMMENT 'corresponding term for the above code, looked up via TRUD',
    diag_notes                      mediumtext COMMENT 'diagnosis notes',
    classification                  varchar(50) COMMENT 'diagnosis classification text. ',
    ranking                         varchar(50) COMMENT 'diagnosis ranking text. ',
    confirmation                    varchar(50) COMMENT 'diagnosis confirmation text. Use to update the verification status',
    axis                            varchar(50) COMMENT 'diagnosis axis text. ',
    location                        varchar(50) COMMENT ' text based location details from ORG_NAME',
    lookup_consultant_personnel_id  int COMMENT 'pre-looked up from diag_prnsl',
    audit_json                      mediumtext  null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_diagnosis PRIMARY KEY (exchange_id, diagnosis_id)
);

CREATE INDEX ix_condition_diagnosis_checksum_helper on condition_diagnosis (diagnosis_id, dt_received);

create table condition_diagnosis_latest
(
    exchange_id                     char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                     datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                 bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    diagnosis_id                    int         NOT NULL COMMENT 'from diagnosis_id, but standardised to remove trailing .00. Joins to condition_DIAGN.diagnosis_id',
    person_id                       int         NOT NULL COMMENT 'from person_id',
    active_ind                      bool        NOT NULL COMMENT 'whether an active record or not (deleted), from active_ind',
    mrn                             varchar(10) NOT NULL COMMENT 'from MRN',
    encounter_id                    int         NOT NULL COMMENT 'from encntr_id, but standardised to remove trailing .00',
    diag_dt_tm                      datetime    COMMENT 'from diag_dt',
    diag_type                       varchar(255) COMMENT 'text based diagnosis type',
    diag_prnsl                      varchar(255) COMMENT 'text based diagnosis performer',
    vocab                           varchar(50) NOT NULL COMMENT 'diagnosis code type, either SNOMED CT or UK ED Subset (Snomed)',
    diag_code                       varchar(50) NOT NULL COMMENT 'diagnosis code of type described by vocab',
    diag_term                       varchar(255) NOT NULL COMMENT 'corresponding term for the above code, looked up via TRUD',
    diag_notes                      mediumtext COMMENT 'diagnosis notes',
    classification                  varchar(50) COMMENT 'diagnosis classification text. ',
    ranking                         varchar(50) COMMENT 'diagnosis ranking text. ',
    confirmation                    varchar(50) COMMENT 'diagnosis confirmation text. Use to update the verification status',
    axis                            varchar(50) COMMENT 'diagnosis axis text. ',
    location                        varchar(50) COMMENT ' text based location details from ORG_NAME',
    lookup_consultant_personnel_id  int COMMENT 'pre-looked up from diag_prnsl',
    audit_json                      mediumtext  null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_diagnosis_latest PRIMARY KEY (diagnosis_id)
);

-- records from DIAGN (UKRWH_CDE_DIAGNOSIS)
create table condition_DIAGN
(
    exchange_id          char(36)   NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received          datetime   NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum      bigint     NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    diagnosis_id         int        NOT NULL COMMENT 'from DIAGNOSIS_ID.  Joins to condition_diagnosis.diagnosis_id',
    active_ind           bool       NOT NULL COMMENT 'whether an active record or not (deleted), from ACTIVE_IND',
    encounter_id         int COMMENT 'from ENCNTR_ID',
    encounter_slice_id   int COMMENT 'from ENCNTR_SLICE_ID',
    diagnosis_dt_tm      datetime COMMENT 'from DIAGNOSIS_DT_TM',
    diagnosis_code_type  varchar(50) COMMENT 'icd-10 or snomed/SNMUKEMED, derived from CONCEPT_CKI_IDENT. format is: type!code',
    diagnosis_code       varchar(50) COMMENT 'icd-10 or snomed/SNMUKEMED code derived from CONCEPT_CKI_IDENT. format is: type!code',
    diagnosis_term       varchar(255) COMMENT 'corresponding term for the above code, looked up via TRUD',
    diagnosis_notes      varchar(255) COMMENT 'free text notes from DIAGNOSIS_TXT',
    diagnosis_type_cd    varchar(50) COMMENT 'from DIAGNOSIS_TYPE_CD, Cerner code set nbr = 17',
    diagnosis_seq_nbr    int COMMENT 'from DIAGNOSIS_SEQ_NBR',
    diag_personnel_id    int COMMENT 'the Id of the person making the diagnosis, from DIAG_HCP_PRSNL_ID',
    lookup_person_id     int COMMENT 'pre-looked up via ENCNTR_ID',
    lookup_mrn           varchar(10) COMMENT 'looked up via ENCNTR_ID',
    audit_json           mediumtext null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_DIAGN PRIMARY KEY (exchange_id, diagnosis_id)
);

CREATE INDEX ix_condition_DIAGN_checksum_helper on condition_DIAGN (diagnosis_id, dt_received);

create table condition_DIAGN_latest
(
    exchange_id          char(36)   NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received          datetime   NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum      bigint     NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    diagnosis_id         int        NOT NULL COMMENT 'from DIAGNOSIS_ID.  Joins to condition_diagnosis.diagnosis_id',
    active_ind           bool       NOT NULL COMMENT 'whether an active record or not (deleted), from ACTIVE_IND',
    encounter_id         int COMMENT 'from ENCNTR_ID',
    encounter_slice_id   int COMMENT 'from ENCNTR_SLICE_ID',
    diagnosis_dt_tm      datetime COMMENT 'from DIAGNOSIS_DT_TM',
    diagnosis_code_type  varchar(50) COMMENT 'icd-10 or snomed/SNMUKEMED, derived from CONCEPT_CKI_IDENT. format is: type!code',
    diagnosis_code       varchar(50) COMMENT 'icd-10 or snomed/SNMUKEMED code derived from CONCEPT_CKI_IDENT. format is: type!code',
    diagnosis_term       varchar(255) COMMENT 'corresponding term for the above code, looked up via TRUD',
    diagnosis_notes      varchar(255) COMMENT 'free text notes from DIAGNOSIS_TXT',
    diagnosis_type_cd    varchar(50) COMMENT 'from DIAGNOSIS_TYPE_CD, Cerner code set nbr = 17',
    diagnosis_seq_nbr    int COMMENT 'from DIAGNOSIS_SEQ_NBR',
    diag_personnel_id    int COMMENT 'the Id of the person making the diagnosis, from DIAG_HCP_PRSNL_ID',
    lookup_person_id     int COMMENT 'pre-looked up via ENCNTR_ID',
    lookup_mrn           varchar(10) COMMENT 'looked up via ENCNTR_ID',
    audit_json           mediumtext null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_DIAGN_latest PRIMARY KEY (diagnosis_id)
);

create table condition_problem
(
    exchange_id          char(36)   NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received          datetime   NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum      bigint     NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    problem_id           int        NOT NULL COMMENT 'unique problem ID',
    person_id            int        NOT NULL COMMENT 'from person_id but standardised to remove trailing .00',
    mrn                  varchar(10) NOT NULL COMMENT 'from MRN',
    onset_dt_tm          datetime COMMENT 'on-set date of problem',
    onset_precision      varchar(50) COMMENT 'The onset date precision',
    updated_by           varchar(50) COMMENT 'Clinician updating the record. Text, so map to Id',
    vocab                varchar(50) COMMENT 'problem code type, either SNOMED CT, ICD-10, Cerner, UK ED Subset (Snomed Description Id),OPCS4,Patient Care',
    problem_code         varchar(50) COMMENT 'snomed description Id',
    problem_term         varchar(255) COMMENT 'problem raw term (not looked up on TRUD)',
    problem_txt          varchar(255) COMMENT 'problem free text, usually the same as the term, annotated_disp',
    classification       varchar(50) COMMENT 'problem classification text',
    confirmation         varchar(50) COMMENT 'problem confirmation text. Use to update the verification status',
    ranking              varchar(50) COMMENT 'problem ranking text ',
    axis                 varchar(50) COMMENT 'diagnosis axis text. ',
    problem_status       varchar(50) COMMENT 'problem status such as Active, Resolved, Inactive, Canceled. From Status_Lifecycle',
    problem_status_date  datetime COMMENT 'the date of the problem status',
    location             varchar(50) COMMENT ' text based location details from ORG_NAME',
    lookup_consultant_personnel_id  int COMMENT 'pre-looked up from updated_by',
    audit_json           mediumtext null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_problem PRIMARY KEY (exchange_id, problem_id)
);

CREATE INDEX ix_condition_problem_checksum_helper on condition_problem (problem_id, dt_received);

create table condition_problem_latest
(
    exchange_id          char(36)   NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received          datetime   NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum      bigint     NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    problem_id           int        NOT NULL COMMENT 'unique problem ID',
    person_id            int        NOT NULL COMMENT 'from person_id but standardised to remove trailing .00',
    mrn                  varchar(10) NOT NULL COMMENT 'from MRN',
    onset_dt_tm           datetime COMMENT 'on-set date of problem',
    onset_precision      varchar(50) COMMENT 'The onset date precision',
    updated_by           varchar(50) COMMENT 'Clinician updating the record. Text, so map to Id',
    vocab                varchar(50) COMMENT 'problem code type, either SNOMED CT, ICD-10, Cerner, UK ED Subset (Snomed Description Id),OPCS4,Patient Care',
    problem_code         varchar(50) COMMENT 'snomed description Id',
    problem_term         varchar(255) COMMENT 'problem raw term (not looked up on TRUD)',
    problem_txt          varchar(255) COMMENT 'problem free text, usually the same as the term, from annotated_disp',
    classification       varchar(50) COMMENT 'problem classification text',
    confirmation         varchar(50) COMMENT 'problem confirmation text. Use to update the verification status',
    ranking              varchar(50) COMMENT 'problem ranking text ',
    axis                 varchar(50) COMMENT 'diagnosis axis text. ',
    problem_status       varchar(50) COMMENT 'problem status such as Active, Resolved, Inactive, Canceled. From Status_Lifecycle',
    problem_status_date  datetime COMMENT 'the date of the problem status',
    location             varchar(50) COMMENT ' text based location details from ORG_NAME',
    lookup_consultant_personnel_id  int COMMENT 'pre-looked up from updated_by',
    audit_json           mediumtext null comment 'Used for Audit Purposes',
    CONSTRAINT pk_condition_problem PRIMARY KEY (problem_id)
);

-- target table for the above tables to populate, cleared down for each exchange
create table condition_target
(
    exchange_id                char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    unique_id                  varchar(255) NOT NULL COMMENT 'unique ID derived from source IDs',
    is_delete                  bool         NOT NULL COMMENT 'if this diagnosis should be deleted or upserted',
    person_id                  int          COMMENT 'person ID for the diagnosis/problem',
    encounter_id               int          COMMENT 'encounter ID for the diagnosis',
    performer_personnel_id     int          COMMENT 'performer ID for the diagnosis/problem. Use updated_by for Problems',
    dt_performed               datetime     COMMENT 'the date of Diagnosis or Problem Onset',
    dt_precision               varchar(50)  COMMENT 'The problem Onset date precision. Problems only',
    condition_code_type        varchar(50)  COMMENT 'SNOMED CT, ICD-10, Cerner, UK ED Subset (Snomed Description Id), OPCS4, Patient Care',
    condition_code             varchar(50),
    condition_term             varchar(255),
    condition_type             varchar(50)  COMMENT 'The type of Diagnosis, either text or coded from Cerner code_set nbr = 17',
    free_text                  mediumtext,
    sequence_number            int,
    parent_condition_unique_id varchar(255),
    classification             varchar(50)  COMMENT 'condition classification text to add to notes',
    confirmation               varchar(50)  COMMENT 'condition confirmation text. Use to update the verification status',
    problem_status             varchar(50)  COMMENT 'problem status such as Active, Resolved, Inactive, Canceled. From Status_Lifecycle.  Indicates a Problem record',
    problem_status_date        datetime     COMMENT 'the date of the problem status',
    ranking                    varchar(50)  COMMENT 'condition ranking text',
    axis                       varchar(50)  COMMENT 'diagnosis axis text',
    location                   varchar(255) COMMENT 'text based location details',
    audit_json                 mediumtext null COMMENT 'Used for Audit Purposes',
    is_confidential            bool         COMMENT 'if this condition should be confidential or not',
    CONSTRAINT pk_condition_target PRIMARY KEY (exchange_id, unique_id)
);

create index ix_duplication_helper on condition_target (person_id, encounter_id, dt_performed, condition_code, unique_id);


-- latest version of every record that is in the target table
create table condition_target_latest
(
    exchange_id                char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    unique_id                  varchar(255) NOT NULL COMMENT 'unique ID derived from source IDs',
    is_delete                  bool         NOT NULL COMMENT 'if this diagnosis should be deleted or upserted',
    person_id                  int          COMMENT 'person ID for the diagnosis/problem',
    encounter_id               int          COMMENT 'encounter ID for the diagnosis',
    performer_personnel_id     int          COMMENT 'performer ID for the diagnosis/problem. Use updated_by for Problems',
    dt_performed               datetime     COMMENT 'the date of Diagnosis or Problem Onset',
    dt_precision               varchar(50)  COMMENT 'The problem Onset date precision. Problems only',
    condition_code_type        varchar(50)  COMMENT 'SNOMED CT, ICD-10, Cerner, UK ED Subset (Snomed Description Id), OPCS4, Patient Care',
    condition_code             varchar(50),
    condition_term             varchar(255),
    condition_type             varchar(50)  COMMENT 'The type of Diagnosis, either text or coded from Cerner code_set nbr = 17',
    free_text                  mediumtext,
    sequence_number            int,
    parent_condition_unique_id varchar(255),
    classification             varchar(50)  COMMENT 'condition classification text to add to notes',
    confirmation               varchar(50)  COMMENT 'condition confirmation text. Use to update the verification status',
    problem_status             varchar(50)  COMMENT 'problem status such as Active, Resolved, Inactive, Canceled. From Status_Lifecycle.  Indicates a Problem record',
    problem_status_date        datetime     COMMENT 'the date of the problem status',
    ranking                    varchar(50)  COMMENT 'condition ranking text',
    axis                       varchar(50)  COMMENT 'diagnosis axis text',
    location                   varchar(255) COMMENT 'text based location details',
    audit_json                 mediumtext null COMMENT 'Used for Audit Purposes',
    is_confidential            bool         COMMENT 'if this condition should be confidential or not',
    CONSTRAINT pk_condition_target PRIMARY KEY (unique_id)
);
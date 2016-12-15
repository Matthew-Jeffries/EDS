/*
	create extensions
*/
create extension "uuid-ossp";

/* 
	create schemas
*/
create schema dictionary;
create schema configuration;
create schema log;

/*
	create tables
*/

create table dictionary.message_type
(
	message_type varchar(100) not null,
	description varchar(100) not null,
	
	constraint dictionary_messagetype_messagetype_pk primary key (message_type),
	constraint dictionary_messagetype_messagetype_ck check (char_length(trim(message_type)) > 0),
	constraint dictionary_messagetype_description_ck check (char_length(trim(description)) > 0)
);

create table configuration.instance
(
	instance_id integer not null,
	instance_name varchar(100) not null,
	description varchar(1000) not null,

	constraint configuration_instance_instanceid_pk primary key (instance_id),
	constraint configuration_instance_instancename_uq unique (instance_name), 
	constraint configuration_instance_instancename_ck check (char_length(trim(instance_name)) > 0),
	constraint configuration_instance_description_uq unique (description),
	constraint configuration_instance_description_ck check (char_length(trim(description)) > 0)
);

create table configuration.channel
(
	channel_id integer not null,
	channel_name varchar(100) not null,
	port_number integer not null,
	is_active boolean not null,
	use_tls boolean not null,
	sending_application varchar(100),
	sending_facility varchar(100),
	receiving_application varchar(100),
	receiving_facility varchar(100),
	notes varchar(1000) not null,

	constraint configuration_channel_channelid_pk primary key (channel_id),
	constraint configuration_channel_channelname_uq unique (channel_name),
	constraint configuration_channel_portnumber_uq unique (port_number),
	constraint configuration_channel_portnumber_ck check (port_number > 0),
	constraint configuration_channel_channelname_ck check (char_length(trim(channel_name)) > 0)
);

create table configuration.channel_message_type
(
	channel_id integer not null,
	message_type varchar(100) not null,
	is_allowed boolean not null,
	
	constraint configuration_channelmessagetype_channelid_messagetype_pk primary key (channel_id, message_type),
	constraint configuration_channelmessagetype_channelid_fk foreign key (channel_id) references configuration.channel (channel_id),
	constraint configuration_channelmessagetype_messagetype_fk foreign key (message_type) references dictionary.message_type (message_type)
);

create table log.connection
(
	connection_id serial not null,
	instance_id integer not null,
	channel_id integer not null,
	local_port integer not null,
	remote_host varchar(100) not null,
	remote_port integer not null,
	connect_date timestamp not null,
	disconnect_date timestamp null,
	
	constraint log_connection_connectionid_pk primary key (connection_id),
	constraint log_connection_instanceid_fk foreign key (instance_id) references configuration.instance (instance_id),
	constraint log_connection_channelid_fk foreign key (channel_id) references configuration.channel (channel_id),
	constraint log_connection_channelid_connectionid_uq unique (channel_id, connection_id),
	constraint log_connection_localport_ck check (local_port > 0),
	constraint log_connection_remotehost_ck check (char_length(trim(remote_host)) > 0), 
	constraint log_connection_remoteport_ck check (remote_port > 0),
	constraint log_connection_connectdate_disconnectdate_ck check ((disconnect_date is null) or (connect_date <= disconnect_date)) 
);

create table log.message
(
	message_id serial not null,
	channel_id integer not null,
	connection_id integer not null,
	log_date timestamp not null,
	message_control_id varchar(100) not null,
	inbound_message_type varchar(100) not null,
	inbound_payload varchar not null,
	outbound_message_type varchar(100) not null,
	outbound_payload varchar not null,
	
	constraint log_message_messageid_pk primary key (message_id),
	constraint log_message_channelid_connectionid_fk foreign key (channel_id, connection_id) references log.connection (channel_id, connection_id),
	constraint log_message_inboundmessagetype_fk foreign key (channel_id, inbound_message_type) references configuration.channel_message_type (channel_id, message_type),
	constraint log_message_outboundmessagetype_fk foreign key (channel_id, outbound_message_type) references configuration.channel_message_type (channel_id, message_type),
	constraint log_message_messagecontrolid_ck check (char_length(trim(message_control_id)) > 0)
);

create table log.error
(
	error_id serial not null,
	error_uuid uuid not null,
	error_count integer not null,
	exception varchar(1000) not null,
	method varchar(1000) not null,
	message text,
	
	constraint log_error_errorid_pk primary key (error_id),
	constraint log_error_erroruuid_fk unique (error_uuid),
	constraint log_error_errorcount_ck check (error_count > 0),
	constraint log_error_exception_method_message_uq unique (exception, method, message)
);

create table log.dead_letter
(
	dead_letter_id serial not null,
	channel_id integer null,
	connection_id integer null,
	log_date timestamp not null,
	local_port integer not null,
	remote_host varchar(100) not null,
	remote_port integer not null,
	sending_application varchar(100) null,
	sending_facility varchar(100) null,
	receiving_application varchar(100) null,
	receiving_facility varchar(100) null,
	message_control_id varchar(100) not null,
	inbound_message_type varchar(100) null,
	inbound_payload varchar not null,
	outbound_message_type varchar(100) null,
	outbound_payload varchar null,
	error_id integer null,
	
	constraint log_deadletter_deadletterid_pk primary key (dead_letter_id),
	constraint log_deadletter_connectionid_fk foreign key (connection_id) references log.connection (connection_id),
	constraint log_deadletter_channelid_fk foreign key (channel_id) references configuration.channel (channel_id),
	constraint log_deadletter_errorid_fk foreign key (error_id) references log.error (error_id)
);

/*
	insert data
*/

insert into dictionary.message_type (message_type, description) values
('ADT^A01', 'Admit / visit notification'),
('ADT^A02', 'Transfer a patient'),
('ADT^A03', 'Discharge/end visit'),
('ADT^A04', 'Register a patient'),
('ADT^A05', 'Pre-admit a patient'),
('ADT^A06', 'Change an outpatient to an inpatient'),
('ADT^A07', 'Change an inpatient to an outpatient'),
('ADT^A08', 'Update patient information'),
('ADT^A09', 'Patient departing - tracking'),
('ADT^A10', 'Patient arriving - tracking'),
('ADT^A11', 'Cancel admit/visit notification'),
('ADT^A12', 'Cancel transfer'),
('ADT^A13', 'Cancel discharge/end visit'),
('ADT^A14', 'Pending admit'),
('ADT^A15', 'Pending transfer'),
('ADT^A16', 'Pending discharge'),
('ADT^A17', 'Swap patients'),
('ADT^A18', 'Merge patient information'),
('ADT^A19', 'Patient query'),
('ADT^A20', 'Bed status update'),
('ADT^A21', 'Patient goes on a "leave of absence"'),
('ADT^A22', 'Patient returns from a "leave of absence"'),
('ADT^A23', 'Delete a patient record'),
('ADT^A24', 'Link patient information'),
('ADT^A25', 'Cancel pending discharge'),
('ADT^A26', 'Cancel pending transfer'),
('ADT^A27', 'Cancel pending admit'),
('ADT^A28', 'Add person information'),
('ADT^A29', 'Delete person information'),
('ADT^A30', 'Merge person information'),
('ADT^A31', 'Update person information'),
('ADT^A32', 'Cancel patient arriving - tracking'),
('ADT^A33', 'Cancel patient departing - tracking'),
('ADT^A34', 'Merge patient information - patient ID only'),
('ADT^A35', 'Merge patient information - account number only'),
('ADT^A36', 'Merge patient information - patient ID and account number'),
('ADT^A37', 'Unlink patient information'),
('ADT^A38', 'Cancel pre-admit'),
('ADT^A39', 'Merge person - external ID'),
('ADT^A40', 'Merge patient - internal ID'),
('ADT^A41', 'Merge account - patient account number'),
('ADT^A42', 'Merge visit - visit number'),
('ADT^A43', 'Move patient information - internal ID'),
('ADT^A44', 'Move account information - patient account number'),
('ADT^A45', 'Move visit information - visit number'),
('ADT^A46', 'Change external ID'),
('ADT^A47', 'Change internal ID'),
('ADT^A48', 'Change alternate patient ID'),
('ADT^A49', 'Change patient account number'),
('ADT^A50', 'Change visit number'),
('ADT^A51', 'Change alternate visit ID'),
('ACK^A01', 'Acknowledgement to admit / visit notification'),
('ACK^A02', 'Acknowledgement to transfer a patient'),
('ACK^A03', 'Acknowledgement to discharge/end visit'),
('ACK^A04', 'Acknowledgement to register a patient'),
('ACK^A05', 'Acknowledgement to pre-admit a patient'),
('ACK^A06', 'Acknowledgement to change an outpatient to an inpatient'),
('ACK^A07', 'Acknowledgement to change an inpatient to an outpatient'),
('ACK^A08', 'Acknowledgement to update patient information'),
('ACK^A09', 'Acknowledgement to patient departing - tracking'),
('ACK^A10', 'Acknowledgement to patient arriving - tracking'),
('ACK^A11', 'Acknowledgement to cancel admit/visit notification'),
('ACK^A12', 'Acknowledgement to cancel transfer'),
('ACK^A13', 'Acknowledgement to cancel discharge/end visit'),
('ACK^A14', 'Acknowledgement to pending admit'),
('ACK^A15', 'Acknowledgement to pending transfer'),
('ACK^A16', 'Acknowledgement to pending discharge'),
('ACK^A17', 'Acknowledgement to swap patients'),
('ACK^A18', 'Acknowledgement to merge patient information'),
('ACK^A19', 'Acknowledgement to patient query'),
('ACK^A20', 'Acknowledgement to bed status update'),
('ACK^A21', 'Acknowledgement to patient goes on a "leave of absence"'),
('ACK^A22', 'Acknowledgement to patient returns from a "leave of absence"'),
('ACK^A23', 'Acknowledgement to delete a patient record'),
('ACK^A24', 'Acknowledgement to link patient information'),
('ACK^A25', 'Acknowledgement to cancel pending discharge'),
('ACK^A26', 'Acknowledgement to cancel pending transfer'),
('ACK^A27', 'Acknowledgement to cancel pending admit'),
('ACK^A28', 'Acknowledgement to add person information'),
('ACK^A29', 'Acknowledgement to delete person information'),
('ACK^A30', 'Acknowledgement to merge person information'),
('ACK^A31', 'Acknowledgement to update person information'),
('ACK^A32', 'Acknowledgement to cancel patient arriving - tracking'),
('ACK^A33', 'Acknowledgement to cancel patient departing - tracking'),
('ACK^A34', 'Acknowledgement to merge patient information - patient ID only'),
('ACK^A35', 'Acknowledgement to merge patient information - account number only'),
('ACK^A36', 'Acknowledgement to merge patient information - patient ID and account number'),
('ACK^A37', 'Acknowledgement to unlink patient information'),
('ACK^A38', 'Acknowledgement to cancel pre-admit'),
('ACK^A39', 'Acknowledgement to merge person - external ID'),
('ACK^A40', 'Acknowledgement to merge patient - internal ID'),
('ACK^A41', 'Acknowledgement to merge account - patient account number'),
('ACK^A42', 'Acknowledgement to merge visit - visit number'),
('ACK^A43', 'Acknowledgement to move patient information - internal ID'),
('ACK^A44', 'Acknowledgement to move account information - patient account number'),
('ACK^A45', 'Acknowledgement to move visit information - visit number'),
('ACK^A46', 'Acknowledgement to change external ID'),
('ACK^A47', 'Acknowledgement to change internal ID'),
('ACK^A48', 'Acknowledgement to change alternate patient ID'),
('ACK^A49', 'Acknowledgement to change patient account number'),
('ACK^A50', 'Acknowledgement to change visit number'),
('ACK^A51', 'Acknowledgement to change alternate visit ID');


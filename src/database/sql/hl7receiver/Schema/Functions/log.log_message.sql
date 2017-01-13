
create or replace function log.log_message
(
	_channel_id integer,
	_connection_id integer,
	_message_control_id varchar(100),
	_message_sequence_number varchar(100),
	_message_date timestamp,
	_inbound_message_type varchar(100),
	_inbound_payload text,
	_outbound_message_type varchar(100),
	_outbound_payload text
)
returns integer
as $$
declare
	_message_id integer;
begin

	insert into log.message
	(
		channel_id,
		connection_id,
		log_date,
		message_control_id,
		message_sequence_number,
		message_date,
		inbound_message_type,
		inbound_payload,
		outbound_message_type,
		outbound_payload,
		notification_status_id
	)
	values
	(
		_channel_id,
		_connection_id,
		now(),
		_message_control_id,
		_message_sequence_number,
		_message_date,
		_inbound_message_type,
		_inbound_payload,
		_outbound_message_type,
		_outbound_payload,
		1
	)
	returning message_id into _message_id;
	
	return _message_id;
	
end;
$$ language plpgsql;

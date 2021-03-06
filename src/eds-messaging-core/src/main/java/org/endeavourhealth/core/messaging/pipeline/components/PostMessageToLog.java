package org.endeavourhealth.core.messaging.pipeline.components;

import org.endeavourhealth.core.configuration.PostMessageToLogConfig;
import org.endeavourhealth.core.database.dal.audit.models.Exchange;
import org.endeavourhealth.core.messaging.pipeline.PipelineComponent;
import org.endeavourhealth.core.messaging.pipeline.PipelineException;
import org.endeavourhealth.transform.common.AuditWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostMessageToLog extends PipelineComponent {
	private static final Logger LOG = LoggerFactory.getLogger(PostMessageToLog.class);

	private PostMessageToLogConfig config;

	public PostMessageToLog(PostMessageToLogConfig config) {
		this.config = config;
	}

	@Override
	public void process(Exchange exchange) throws PipelineException {
		String eventType = config.getEventType();

		try {
			AuditWriter.writeExchangeEvent(exchange, eventType);

		} catch (Exception e) {
			throw new PipelineException("Failed to write exchange " + exchange.getId() + " " + eventType + " to audit DB", e);
		}
	}

	/*public void process(Exchange exchange) throws PipelineException {
		AuditEvent auditEvent = AuditEvent.fromString(config.getEventType());

		try {
			AuditWriter.writeAuditEvent(exchange, auditEvent);
			LOG.debug("Message written to outbound log");
		} catch (Exception e) {
			LOG.error("Error writing exchange to audit", e);
			// throw new PipelineException(e.getMessage());
		}
	}*/

}

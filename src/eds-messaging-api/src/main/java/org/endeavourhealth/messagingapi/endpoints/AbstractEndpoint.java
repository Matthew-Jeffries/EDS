package org.endeavourhealth.messagingapi.endpoints;

import org.apache.http.HttpStatus;
import org.endeavourhealth.core.configuration.Pipeline;
import org.endeavourhealth.core.messaging.exchange.Exchange;
import org.endeavourhealth.core.messaging.pipeline.PipelineProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.UUID;

public abstract class AbstractEndpoint {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEndpoint.class);

	protected Response Process(HttpHeaders headers, String body, Pipeline pipeline) {

		Exchange exchange = new Exchange(UUID.randomUUID(), body);

		for (String key : headers.getRequestHeaders().keySet())
			exchange.setHeader(key, headers.getHeaderString(key));

		PipelineProcessor processor = new PipelineProcessor(pipeline);
		if (processor.execute(exchange)) {
			return Response
					.ok()
					.entity(exchange.getBody())
					.build();
		} else {

			//possibly take out later, but for testing purposes, having visibility of these is useful
			if (exchange.getException() != null) {
				LOG.error("Error processing exchange " + exchange.getExchangeId(), exchange.getException());
			}

			return Response
					.status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
					.entity(exchange.getException().getMessage())
					.build();
		}
	}
}

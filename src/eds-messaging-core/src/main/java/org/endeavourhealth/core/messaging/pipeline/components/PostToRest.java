package org.endeavourhealth.core.messaging.pipeline.components;

import org.apache.http.HttpStatus;
import org.endeavourhealth.core.configuration.PostToRestConfig;
import org.endeavourhealth.core.database.dal.audit.models.Exchange;
import org.endeavourhealth.core.database.dal.audit.models.HeaderKeys;
import org.endeavourhealth.core.messaging.pipeline.PipelineComponent;
import org.endeavourhealth.core.messaging.pipeline.PipelineException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import java.util.Set;

public class PostToRest extends PipelineComponent {
	private static final Logger LOG = LoggerFactory.getLogger(PostToRest.class);

	private PostToRestConfig config;

	public PostToRest(PostToRestConfig config) {
		this.config = config;
	}

	@Override
	public void process(Exchange exchange) throws PipelineException {
		String responseAddress = exchange.getHeader(HeaderKeys.DestinationAddress);
		if (responseAddress == null || responseAddress.isEmpty()) {
			LOG.info("Response address not provided");
			return;
		}

		String[] addressList = responseAddress.split(",", -1);

		// Is there a restricted header list?
		String[] headersToSend;
		if (config.getSendHeaders() == null || config.getSendHeaders().isEmpty()) {
			Set<String> headerKeys = exchange.getHeaders().keySet();
			headersToSend = headerKeys.toArray(new String[headerKeys.size()]);
		} else
			headersToSend = config.getSendHeaders().split(",", -1);

		for (String address : addressList) {
			Client client = ClientBuilder.newClient(new ClientConfig().register(LoggingFeature.class));
			WebTarget webTarget = client.target(address);

			String contentType = exchange.getHeader(HeaderKeys.ContentType);

			if (contentType == null)
				contentType = "text/json";

			Invocation.Builder invocationBuilder = webTarget.request(contentType);

			for (String key : headersToSend) {
				String headerValue = exchange.getHeader(key);
				if (headerValue != null)
					invocationBuilder.header(key, headerValue);
			}

			Entity entity = Entity.entity(exchange.getBody(), contentType);

			Response response = invocationBuilder.post(entity);

			//TODO - handle response properly (do not overwrite Exchange body)
			//exchange.setBody(response.readEntity(String.class));

			if (response.getStatus() == HttpStatus.SC_OK)
				LOG.debug("Message posted to REST endpoint");
			else {
				LOG.error("Error posting to REST endpoint");
				throw new PipelineException(exchange.getBody());
			}
		}
	}
}

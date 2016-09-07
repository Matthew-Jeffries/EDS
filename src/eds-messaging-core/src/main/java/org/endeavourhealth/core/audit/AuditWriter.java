package org.endeavourhealth.core.audit;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.endeavourhealth.core.cache.ObjectMapperPool;
import org.endeavourhealth.core.data.audit.AuditRepository;
import org.endeavourhealth.core.data.audit.models.ExchangeEvent;
import org.endeavourhealth.core.messaging.exchange.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public final class AuditWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AuditWriter.class);

    private static final AuditRepository repository = new AuditRepository();

    public static void writeAuditEvent(Exchange ex, AuditEvent event) throws Exception {

        org.endeavourhealth.core.data.audit.models.Exchange exchangeToSave = null;
        ExchangeEvent eventToSave = null;

        UUID uuid = ex.getExchangeId();
        //if logging the receive of an exchange, log the exchange itself
        if (event == AuditEvent.RECEIVE) {

            String body = ex.getBody();

            //use jackson to write the headers to JSON
            Map<String, String> headers = ex.getHeaders();
            String headersJson = ObjectMapperPool.getInstance().writeValueAsString(headers);

            exchangeToSave = new org.endeavourhealth.core.data.audit.models.Exchange();
            exchangeToSave.setTimestamp(new Date());
            exchangeToSave.setExchangeId(uuid);
            exchangeToSave.setHeaders(headersJson);
            exchangeToSave.setBody(body);
        }

        eventToSave = new ExchangeEvent();
        eventToSave.setTimestamp(new Date());
        eventToSave.setExchangeId(uuid);
        eventToSave.setEvent(new Integer(event.getValue()));

        repository.save(exchangeToSave, eventToSave);
    }


}

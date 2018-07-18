package org.endeavourhealth.messagingapi.endpoints;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.swagger.annotations.ApiParam;
import org.apache.http.HttpStatus;
import org.endeavourhealth.common.cache.ObjectMapperPool;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.common.security.SecurityUtils;
import org.endeavourhealth.core.database.dal.DalProvider;
import org.endeavourhealth.core.database.dal.admin.LibraryRepositoryHelper;
import org.endeavourhealth.core.database.dal.admin.ServiceDalI;
import org.endeavourhealth.core.database.dal.admin.SystemHelper;
import org.endeavourhealth.core.database.dal.audit.SubscriberApiAuditDalI;
import org.endeavourhealth.core.database.dal.audit.models.SubscriberApiAudit;
import org.endeavourhealth.core.database.dal.eds.PatientLinkDalI;
import org.endeavourhealth.core.database.dal.eds.PatientSearchDalI;
import org.endeavourhealth.core.database.dal.eds.models.PatientSearch;
import org.endeavourhealth.core.database.dal.subscriberTransform.PseudoIdDalI;
import org.endeavourhealth.core.fhirStorage.FhirSerializationHelper;
import org.endeavourhealth.core.fhirStorage.JsonServiceInterfaceEndpoint;
import org.endeavourhealth.core.messaging.pipeline.PipelineException;
import org.endeavourhealth.core.xml.QueryDocument.*;
import org.hl7.fhir.instance.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.util.*;

@Path("/subscriber")
public class SubscriberApi {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriberApi.class);

    private static final String FRAILTY_CODE = "289999999105";
    private static final String FRAILTY_TERM = "Potentially frail";
    private static final String SUBSCRIBER_SYSTEM_NAME = "JSON_API"; //"Subscriber_Rest_API";

    private static SubscriberApiAuditDalI apiAuditDal = DalProvider.factorySubscriberAuditApiDal();

    @GET
    @Path("/{resourceType}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"dds_api_read_only"})
    public Response getResources(@Context HttpServletRequest request,
                                 @Context SecurityContext sc,
                                @Context UriInfo uriInfo,
                                @ApiParam(value="Resource Type") @PathParam(value = "resourceType") String resourceTypeRequested,
                                @ApiParam(value="ODS Code") @HeaderParam(value = "OdsCode") String headerOdsCode,
                                @ApiParam(value="Auth Token") @HeaderParam(value = "Authorization") String headerAuthToken) throws Exception{

        LOG.info("Subscriber API request received with resource type = [" + resourceTypeRequested + "] and ODS code [" + headerOdsCode + "]");

        SubscriberApiAudit audit = createAudit(request, sc, uriInfo);

        try {
            String subjectNhsNumber = null;
            String code = null;

            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            for (String key : params.keySet()) {
                String value = params.getFirst(key);
                LOG.info("Request parameter [" + key + "] = [" + value + "]");

                if (key.equalsIgnoreCase("subject")) {
                    subjectNhsNumber = value;

                } else if (key.equalsIgnoreCase("code")) {
                    code = value;

                } else {
                    return createErrorResponse(OperationOutcome.IssueType.STRUCTURE, "Invalid parameter '" + key + "'", audit);
                }
            }

            //validate all expected parameters and headers are there
            if (Strings.isNullOrEmpty(resourceTypeRequested)) {
                return createErrorResponse(OperationOutcome.IssueType.REQUIRED, "Missing resource type requested from URL path", audit);
            }

            if (Strings.isNullOrEmpty(headerOdsCode)) {
                return createErrorResponse(OperationOutcome.IssueType.REQUIRED, "Missing OdsCode from request headers", audit);
            }

            if (Strings.isNullOrEmpty(subjectNhsNumber)) {
                return createErrorResponse(OperationOutcome.IssueType.REQUIRED, "Missing subject parameter", audit);
            }

            if (Strings.isNullOrEmpty(code)) {
                return createErrorResponse(OperationOutcome.IssueType.REQUIRED, "Missing code parameter", audit);
            }

            //validate the parameters match what we're expecting
            if (!resourceTypeRequested.equalsIgnoreCase("flag")) {
                return createErrorResponse(OperationOutcome.IssueType.NOTSUPPORTED, "Only flag FHIR resource types can be requested", audit);
            }

            if (!code.equalsIgnoreCase(FRAILTY_CODE)) {
                return createErrorResponse(OperationOutcome.IssueType.NOTSUPPORTED, "Only code " + FRAILTY_CODE + " can be requested", audit);
            }

            //find the service the request is being made for
            ServiceDalI serviceDalI = DalProvider.factoryServiceDal();
            org.endeavourhealth.core.database.dal.admin.models.Service requestingService = serviceDalI.getByLocalIdentifier(headerOdsCode);
            if (requestingService == null) {
                return createErrorResponse(OperationOutcome.IssueType.VALUE, "Unknown requesting ODS code '" + headerOdsCode + "'", audit);
            }
            UUID serviceId = requestingService.getId();

            //validate that the keycloak user (from the token) is permitted to make requests on behalf of the ODS code being requested for
            Set<String> serviceIds = SecurityUtils.getUserAllowedOrganisationIdsFromSecurityContext(sc);
            /*LOG.debug("Found " + serviceIds + " services IDs from Keycloak and our service ID = " + serviceId.toString());
            for (String s: serviceIds) {
                LOG.debug("ServiceId = " + s + " match = " + (s.equals(serviceId.toString())));
            }*/
            if (!serviceIds.contains(serviceId.toString())) {
                return createErrorResponse(OperationOutcome.IssueType.BUSINESSRULE, "You are not permitted to request for ODS code " + headerOdsCode, audit);
            }
            /*if (!headerOdsCode.equalsIgnoreCase("111TESTORG")
                    && !headerOdsCode.equalsIgnoreCase("YGMX6")
                    && !headerOdsCode.equalsIgnoreCase("ADASTRA")
                    && !headerOdsCode.equalsIgnoreCase("NTP")
                    && !headerOdsCode.equalsIgnoreCase("NKB")
                    && !headerOdsCode.equalsIgnoreCase("8HD62")
                    && !headerOdsCode.equalsIgnoreCase("RRU")
                    && !headerOdsCode.equalsIgnoreCase("NLO")) {
                return createErrorResponse(OperationOutcome.IssueType.BUSINESSRULE, "You are not permitted to request for ODS code " + headerOdsCode, audit);
            }*/

            UUID systemId = SystemHelper.findSystemUuid(requestingService, SUBSCRIBER_SYSTEM_NAME);
            if (systemId == null) {
                return createErrorResponse(OperationOutcome.IssueType.VALUE, "Requesting organisation not configured for " + SUBSCRIBER_SYSTEM_NAME, audit);
            }

            //ensure the service is a valid subscriber to at least one protocol
            List<Protocol> protocols = getProtocolsForSubscriberService(serviceId.toString(), systemId.toString());
            if (protocols.isEmpty()) {
                return createErrorResponse(OperationOutcome.IssueType.VALUE, "No valid subscriber agreement found for requesting ODS code '" + headerOdsCode + "' and system " + SUBSCRIBER_SYSTEM_NAME, audit);
            }

            //the below only works properly if there's a single protocol. To support multiple protocols,
            //it'll need to calculate the frailty against EACH subscriber DB and then return the one with the highest risk
            if (protocols.size() > 1) {
                return createErrorResponse(OperationOutcome.IssueType.PROCESSING, "No support for multiple subscriber protocols in frailty calculation", audit);
            }

            Protocol protocol = protocols.get(0);
            Set<String> publisherServiceIds = getPublisherServiceIdsForProtocol(protocol);

            //find patient
            PatientSearchDalI patientSearchDal = DalProvider.factoryPatientSearchDal();
            List<PatientSearch> results = patientSearchDal.searchByNhsNumber(publisherServiceIds, subjectNhsNumber);

            if (results.isEmpty()) {
                return createErrorResponse(OperationOutcome.IssueType.NOTFOUND, "No patient record could be found for NHS number " + subjectNhsNumber, audit);
            }

            //calculate the flag (note that returning a NULL flag is a valid result if the patient isn't frail)
            try {

                String enterpriseEndpoint = getEnterpriseEndpoint(requestingService, systemId);
                if (!Strings.isNullOrEmpty(enterpriseEndpoint)) {
                    LOG.debug("Calculating frailty using " + enterpriseEndpoint);
                    return calculateFrailtyFlagLive(enterpriseEndpoint, results, uriInfo, params, headerAuthToken, audit);

                } else {
                    LOG.debug("Using DUMMY mechanism to calculate Frailty");
                    return calculateFrailtyFlagDummy(results, params, audit);
                }

            } catch (Exception ex) {
                //any exception from calculating the flag should be returned as a processing error
                String err = ex.getMessage();
                return createErrorResponse(OperationOutcome.IssueType.PROCESSING, err, audit);
            }

        } finally {
            //save the audit, but if there's an error saving, catch and log here, so the API response isn't affected
            try {
                apiAuditDal.saveSubscriberApiAudit(audit);
            } catch (Exception ex) {
                LOG.error("Error saving audit", ex);
            }
        }
    }



    private static SubscriberApiAudit createAudit(HttpServletRequest request, SecurityContext sc, UriInfo uriInfo) {
        SubscriberApiAudit audit = new SubscriberApiAudit();
        audit.setTimestmp(new Date());

        UUID userUuid = SecurityUtils.getCurrentUserId(sc);
        audit.setUserUuid(userUuid);

        String requestPath = uriInfo.getRequestUri().toString();
        audit.setRequestPath(requestPath);

        String requestAddress = request.getRemoteAddr();
        audit.setRemoteAddress(requestAddress);

        List<String> headerTokens = new ArrayList<>();
        java.util.Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();

            //ignore these two headers, as they don't give us anything useful
            if (header.equals("host")
                    || header.equals("authorization")) {
                continue;
            }
            String headerVal = request.getHeader(header);
            headerTokens.add(header + "=" + headerVal);
        }
        String headerStr = String.join(";", headerTokens);
        audit.setRequestHeaders(headerStr);

        return audit;
    }

    private String getEnterpriseEndpoint(org.endeavourhealth.core.database.dal.admin.models.Service service, UUID systemId) throws Exception {

        List<JsonServiceInterfaceEndpoint> serviceEndpoints = ObjectMapperPool.getInstance().readValue(service.getEndpoints(), new TypeReference<List<JsonServiceInterfaceEndpoint>>() {});
        for (JsonServiceInterfaceEndpoint serviceEndpoint: serviceEndpoints) {
            if (serviceEndpoint.getSystemUuid().equals(systemId)) {
                return serviceEndpoint.getEndpoint();
            }
        }

        return null;
    }

    private Response createSuccessResponse(Flag frailtyFlag, MultivaluedMap<String, String> requestParams, SubscriberApiAudit audit) throws Exception {

        //the response object is a parameters resource, containing the
        //original request parameters, plus a special parameter containing the
        //response resource (this pattern is defined on the FHIR site)
        Parameters parameters = new Parameters();

        for (String key: requestParams.keySet()) {
            String value = requestParams.getFirst(key);

            Parameters.ParametersParameterComponent comp = parameters.addParameter();
            comp.setName(key);
            comp.setValue(new StringType(value));
        }

        Parameters.ParametersParameterComponent comp = parameters.addParameter();
        comp.setName("return");
        comp.setResource(frailtyFlag); //note that this may be null if not frail

        String json = FhirSerializationHelper.serializeResource(parameters);
        LOG.info("Returning success response: " + json);

        Response response = Response
                .ok()
                .entity(json)
                .build();

        updateAudit(audit, response);

        return response;
    }

    private Response createErrorResponse(OperationOutcome.IssueType issueType, String message, SubscriberApiAudit audit) throws Exception {

        OperationOutcome outcome = new OperationOutcome();
        OperationOutcome.OperationOutcomeIssueComponent issue = outcome.addIssue();
        issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        issue.setCode(issueType);

        CodeableConcept concept = new CodeableConcept();
        concept.setText(message);
        issue.setDetails(concept);

        String json = FhirSerializationHelper.serializeResource(outcome);
        LOG.info("Returning error response: " + json);

        Response response = Response
                .status(Response.Status.BAD_REQUEST)
                .entity(json)
                .build();

        updateAudit(audit, response);

        return response;
    }

    private void updateAudit(SubscriberApiAudit audit, Response response) {
        int statusCode = response.getStatus();
        audit.setResponseCode(new Integer(statusCode));

        if (response.hasEntity()) {
            String json = (String)response.getEntity();
            audit.setResponseBody(json);
        }
    }


    private Response calculateFrailtyFlagLive(String enterpriseEndpoint, List<PatientSearch> results, UriInfo uriInfo, MultivaluedMap<String, String> requestParams, String headerAuthToken, SubscriberApiAudit audit) throws Exception {

        //find a single pseudo ID for the patient IDs found from the NHS number
        String matchedPseudoId = null;

        PseudoIdDalI pseudoIdDal = DalProvider.factoryPseudoIdDal(enterpriseEndpoint);

        for (PatientSearch result: results) {
            UUID patientId = result.getPatientId();
            String pseudoId = pseudoIdDal.findPseudoId(patientId.toString());
            if (!Strings.isNullOrEmpty(pseudoId)) {

                if (matchedPseudoId == null) {
                    matchedPseudoId = pseudoId;

                } else if (!matchedPseudoId.equals(pseudoId)) {
                    return createErrorResponse(OperationOutcome.IssueType.PROCESSING, "Multiple IDs found for patients with NHS number " + result.getNhsNumber(), audit);
                }
            }
        }

        //make the call to the Enterprise web server
        JsonNode jsonConfig = ConfigManager.getConfigurationAsJson(enterpriseEndpoint, "db_subscriber");
        JsonNode jsonServer = jsonConfig.get("web_server");
        String serverUrl = jsonServer.asText();

        //work out the path we should call on the Enterprise server based on our path
        //this is only necessary because this endpoint may be called with either realm, so we need
        //to ensure the down-stream call uses the same realm, since we just pass through the original Keycloak token
        String enterprisePath = null;
        String requestPath = uriInfo.getRequestUri().toString();
        if (requestPath.indexOf("machine-api") > -1) {
            enterprisePath = "machine-api/cohort/getFrailty";
        } else {
            enterprisePath = "api/cohort/getFrailty";
        }

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(serverUrl).path(enterprisePath);
        LOG.debug("Making call to " + target.getUri());
        target = target.queryParam("pseudoId", matchedPseudoId);

        try {
            Response response = target
                    .request()
                    .header("Authorization", headerAuthToken)
                    .get();

            if (response.getStatus() == HttpStatus.SC_OK) {
                String calculatedFrailty = response.readEntity(String.class);
                LOG.debug("Received response [" + calculatedFrailty + "]");

                if (calculatedFrailty.equalsIgnoreCase("NONE")) {
                    return createSuccessResponse(null, requestParams, audit);

                } else if (calculatedFrailty.equalsIgnoreCase("MILD")
                        || calculatedFrailty.equalsIgnoreCase("MODERATE")
                        || calculatedFrailty.equalsIgnoreCase("SEVERE")) {

                    CodeableConcept codeableConcept = new CodeableConcept();
                    Coding coding = codeableConcept.addCoding();
                    coding.setCode(FRAILTY_CODE);
                    coding.setDisplay(FRAILTY_TERM);
                    codeableConcept.setText(FRAILTY_TERM);

                    Flag flag = new Flag();
                    flag.setStatus(Flag.FlagStatus.ACTIVE);
                    flag.setCode(codeableConcept);
                    return createSuccessResponse(null, requestParams, audit);

                } else {
                    throw new Exception("Unsupported frailty calculated value [" + calculatedFrailty + "]");
                }

            } else {
                String msg = "HTTP error " + response.getStatus() + " calling into frailty calculation service";

                try {
                    String errResponse = response.readEntity(String.class);
                    if (!Strings.isNullOrEmpty(errResponse)) {
                        msg += " (" + errResponse + ")";
                    }
                } catch (Exception ex) {
                    //do nothing
                }

                return createErrorResponse(OperationOutcome.IssueType.PROCESSING, msg, audit);
            }

        } catch (Exception ex) {
            String msg = ex.getMessage();
            return createErrorResponse(OperationOutcome.IssueType.EXCEPTION, msg, audit);
        }

    }

    /**
     * function to calculate the frailty flag of a person in Discovery, using only data held
     * by the publisher service IDs supplied. If a frailty flag can't be calculated, an exception
     * is thrown with the reason for the failure. If the patient is calculated to not be frail, null is returned.
     *
     * Note: this returns a Flag from the DSTU2 FHIR library, but this is compatible with STU3,
     * so receivers of this flag shouldn't need to worry about it being DSTU2.
     */
    private Response calculateFrailtyFlagDummy(List<PatientSearch> searchResults, MultivaluedMap<String, String> requestParams, SubscriberApiAudit audit) throws Exception {

        //ensure all results, map to the same PERSON
        PatientLinkDalI patientLinkDal = DalProvider.factoryPatientLinkDal();

        String personId = null;
        for (PatientSearch result: searchResults) {
            String patientId = result.getPatientId().toString();

            String thisPersonId = patientLinkDal.getPersonId(patientId);
            if (personId == null
                    || personId.equals(thisPersonId)) {
                personId = thisPersonId;

            } else {
                //this shouldn't happen while we continue to match patient-person on NHS number, but if that changes, this will be relevant
                return createErrorResponse(OperationOutcome.IssueType.PROCESSING, "Multiple person records exist for patients with NHS number " + result.getNhsNumber(), audit);
            }
        }

        //there are four potential outcomes, so use the hashcode of the person ID to determine
        //which result should be returned, so it's consistent for NHS numbers
        long hashCode = UUID.fromString(personId).hashCode();
        long result = hashCode % 4;

        if (result == 0) {
            //error
            throw new Exception("Error calculating frailty flag");

        } else if (result == 1) {
            //not enough data to calculate accurately
            throw new Exception("Insufficient data to calculate frailty");

        } else if (result == 2) {
            //not frail
            return createSuccessResponse(null, requestParams, audit);

        } else {
            //potentially frail
            CodeableConcept codeableConcept = new CodeableConcept();
            Coding coding = codeableConcept.addCoding();
            coding.setCode(FRAILTY_CODE);
            coding.setDisplay(FRAILTY_TERM);
            codeableConcept.setText(FRAILTY_TERM);

            Flag flag = new Flag();
            flag.setStatus(Flag.FlagStatus.ACTIVE);
            flag.setCode(codeableConcept);

            return createSuccessResponse(flag, requestParams, audit);
        }
    }

    private static Set<String> getPublisherServiceIdsForProtocol(Protocol protocol) {
        Set<String> ret = new HashSet<>();

        for (ServiceContract serviceContract : protocol.getServiceContract()) {
            if (serviceContract.getType().equals(ServiceContractType.PUBLISHER)
                    && serviceContract.getActive() == ServiceContractActive.TRUE) {

                Service service = serviceContract.getService();
                ret.add(service.getUuid());
            }
        }

        return ret;
    }

    private static List<Protocol> getProtocolsForSubscriberService(String serviceUuid, String systemUuid) throws PipelineException {

        try {
            List<Protocol> ret = new ArrayList<>();

            List<LibraryItem> libraryItems = LibraryRepositoryHelper.getProtocolsByServiceId(serviceUuid, systemUuid);

            //the above fn will return is all protocols where the service and system are present, but we want to filter
            //that down to only ones where our service and system are an active publisher
            for (LibraryItem libraryItem: libraryItems) {
                Protocol protocol = libraryItem.getProtocol();
                if (protocol.getEnabled() == ProtocolEnabled.TRUE) { //added missing check

                    for (ServiceContract serviceContract : protocol.getServiceContract()) {
                        if (serviceContract.getType().equals(ServiceContractType.SUBSCRIBER)
                                && serviceContract.getService().getUuid().equals(serviceUuid)
                                && serviceContract.getSystem().getUuid().equals(systemUuid)
                                && serviceContract.getActive() == ServiceContractActive.TRUE) { //added missing check

                            ret.add(protocol);
                            break;
                        }
                    }
                }
            }

            return ret;

        } catch (Exception ex) {
            throw new PipelineException("Error getting protocols for service " + serviceUuid, ex);
        }
    }


}

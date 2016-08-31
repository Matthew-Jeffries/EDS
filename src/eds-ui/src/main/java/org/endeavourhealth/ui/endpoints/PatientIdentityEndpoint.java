package org.endeavourhealth.ui.endpoints;

import com.google.common.base.Strings;
import org.endeavourhealth.core.data.admin.LibraryRepository;
import org.endeavourhealth.core.data.admin.ServiceRepository;
import org.endeavourhealth.core.data.admin.models.ActiveItem;
import org.endeavourhealth.core.data.admin.models.Item;
import org.endeavourhealth.core.data.admin.models.Service;
import org.endeavourhealth.core.data.ehr.PatientIdentifierRepository;
import org.endeavourhealth.core.data.ehr.models.PatientIdentifierByLocalId;
import org.endeavourhealth.core.data.ehr.models.PatientIdentifierByNhsNumber;
import org.endeavourhealth.core.data.ehr.models.PatientIdentifierByPatientId;
import org.endeavourhealth.ui.framework.exceptions.BadRequestException;
import org.endeavourhealth.ui.json.JsonPatientIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/patientIdentity")
public final class PatientIdentityEndpoint extends AbstractEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(PatientIdentityEndpoint.class);

    private static final PatientIdentifierRepository identifierRepository = new PatientIdentifierRepository();
    private static final ServiceRepository serviceRepository = new ServiceRepository();
    private static final LibraryRepository libraryRepository = new LibraryRepository();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/byLocalIdentifier")
    public Response byLocalIdentifier(@Context SecurityContext sc,
                         @QueryParam("serviceId") String serviceIdStr,
                         @QueryParam("systemId") String systemIdStr,
                         @QueryParam("localId") String localId) throws Exception {

        super.setLogbackMarkers(sc);

        if (Strings.isNullOrEmpty(serviceIdStr)) {
            throw new BadRequestException("A service must be selected");
        }
        if (Strings.isNullOrEmpty(systemIdStr)) {
            throw new BadRequestException("A system must be selected");
        }
        if (Strings.isNullOrEmpty(localId)) {
            throw new BadRequestException("Local ID must be entered");
        }

        UUID serviceId = UUID.fromString(serviceIdStr);
        UUID systemId = UUID.fromString(systemIdStr);

        String serviceName = getServiceNameForId(serviceId);
        String systemName = getSystemNameForId(systemId);

        List<JsonPatientIdentifier> ret = new ArrayList<>();

        List<PatientIdentifierByLocalId> identifiers = identifierRepository.getForLocalId(serviceId, systemId, localId);
        for (PatientIdentifierByLocalId identifier: identifiers) {

            JsonPatientIdentifier json = new JsonPatientIdentifier();
            json.setServiceId(serviceId);
            json.setServiceName(serviceName);
            json.setSystemId(systemId);
            json.setSystemName(systemName);
            json.setNhsNumber(identifier.getNhsNumber());
            json.setForenames(identifier.getForenames());
            json.setSurname(identifier.getSurname());
            json.setDateOfBirth(identifier.getDateOfBirth());
            json.setPostcode(identifier.getPostcode());
            json.setGender(identifier.getGender().getDisplay());
            json.setPatientId(identifier.getPatientId());
            json.setLocalId(identifier.getLocalId());
            json.setLocalIdSystem(identifier.getLocalIdSystem());

            ret.add(json);
        }

        clearLogbackMarkers();

        return Response
                .ok()
                .entity(ret)
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/byNhsNumber")
    public Response byNhsNumber(@Context SecurityContext sc, @QueryParam("nhsNumber") String nhsNumber) throws Exception {
        super.setLogbackMarkers(sc);

        if (Strings.isNullOrEmpty(nhsNumber)) {
            throw new BadRequestException("NHS number must be entered");
        }

        List<JsonPatientIdentifier> ret = new ArrayList<>();

        List<PatientIdentifierByNhsNumber> identifiers = identifierRepository.getForNhsNumber(nhsNumber);
        for (PatientIdentifierByNhsNumber identifier: identifiers) {

            UUID serviceId = identifier.getServiceId();
            UUID systemId = identifier.getSystemId();

            String serviceName = getServiceNameForId(serviceId);
            String systemName = getSystemNameForId(systemId);

            JsonPatientIdentifier json = new JsonPatientIdentifier();
            json.setServiceId(serviceId);
            json.setServiceName(serviceName);
            json.setSystemId(systemId);
            json.setSystemName(systemName);
            json.setNhsNumber(identifier.getNhsNumber());
            json.setForenames(identifier.getForenames());
            json.setSurname(identifier.getSurname());
            json.setDateOfBirth(identifier.getDateOfBirth());
            json.setPostcode(identifier.getPostcode());
            json.setGender(identifier.getGender().getDisplay());
            json.setPatientId(identifier.getPatientId());
            json.setLocalId(identifier.getLocalId());
            json.setLocalIdSystem(identifier.getLocalIdSystem());

            ret.add(json);
        }

        clearLogbackMarkers();

        return Response
                .ok()
                .entity(ret)
                .build();
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/byPatientId")
    public Response byPatientId(@Context SecurityContext sc, @QueryParam("patientId") String patientIdStr) throws Exception {
        super.setLogbackMarkers(sc);

        if (Strings.isNullOrEmpty(patientIdStr)) {
            throw new BadRequestException("Patient ID must be entered");
        }

        UUID patientId = null;
        try {
            patientId = UUID.fromString(patientIdStr);
        } catch (IllegalArgumentException ex) {
            //do nothing if it's not a valid UUID
        }

        List<JsonPatientIdentifier> ret = new ArrayList<>();

        if (patientId != null) {

            PatientIdentifierByPatientId identifier = identifierRepository.getMostRecentByPatientId(patientId);
            if (identifier != null) {

                UUID serviceId = identifier.getServiceId();
                UUID systemId = identifier.getSystemId();

                String serviceName = getServiceNameForId(serviceId);
                String systemName = getSystemNameForId(systemId);

                JsonPatientIdentifier json = new JsonPatientIdentifier();
                json.setServiceId(serviceId);
                json.setServiceName(serviceName);
                json.setSystemId(systemId);
                json.setSystemName(systemName);
                json.setNhsNumber(identifier.getNhsNumber());
                json.setForenames(identifier.getForenames());
                json.setSurname(identifier.getSurname());
                json.setDateOfBirth(identifier.getDateOfBirth());
                json.setPostcode(identifier.getPostcode());
                json.setGender(identifier.getGender().getDisplay());
                json.setPatientId(identifier.getPatientId());
                json.setLocalId(identifier.getLocalId());
                json.setLocalIdSystem(identifier.getLocalIdSystem());

                ret.add(json);

            }
        }

        clearLogbackMarkers();

        return Response
                .ok()
                .entity(ret)
                .build();
    }

    private static String getServiceNameForId(UUID serviceId) {
        try {
            Service service = serviceRepository.getById(serviceId);
            return service.getName();
        } catch (NullPointerException ex ) {
            LOG.error("Failed to find service for ID {}", serviceId);
            return "UNKNOWN";
        }

    }
    private static String getSystemNameForId(UUID systemId) {
        try {
            ActiveItem activeItem = libraryRepository.getActiveItemByItemId(systemId);
            Item item = libraryRepository.getItemByKey(systemId, activeItem.getAuditId());
            return item.getTitle();
        } catch (NullPointerException ex) {
            LOG.error("Failed to find system for ID {}", systemId);
            return "UNKNOWN";
        }


    }
}


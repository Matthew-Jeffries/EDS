package org.endeavourhealth.transform.common.idmappers;

import org.endeavourhealth.transform.common.IdHelper;
import org.endeavourhealth.transform.fhir.ReferenceComponents;
import org.endeavourhealth.transform.fhir.ReferenceHelper;
import org.hl7.fhir.instance.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public abstract class BaseIdMapper {
    private static final Logger LOG = LoggerFactory.getLogger(BaseIdMapper.class);

    /**
     * maps the main ID of any resource
     */
    protected void mapResourceId(Resource resource, UUID serviceId, UUID systemId) {

        if (!resource.hasId()) {
            return;
        }

        String newId = IdHelper.getOrCreateEdsResourceIdString(serviceId, systemId, resource.getResourceType(), resource.getId());
        resource.setId(newId);
    }

    /**
     * maps the IDs in any extensions of a resource
     */
    protected void mapExtensions(DomainResource resource, UUID serviceId, UUID systemId) {

        if (!resource.hasExtension()) {
            return;
        }

        for (Extension extension: resource.getExtension()) {
            if (extension.hasValue()
                && extension.getValue() instanceof Reference) {
                mapReference((Reference)extension.getValue(), resource, serviceId, systemId);
            }
        }
    }

    /**
     * maps the IDs in any identifiers of a resource
     */
    protected void mapIdentifiers(List<Identifier> identifiers, Resource resource, UUID serviceId, UUID systemId) {
        for (Identifier identifier: identifiers) {
            if (identifier.hasAssigner()) {
                mapReference(identifier.getAssigner(), resource, serviceId, systemId);
            }
        }
    }

    /**
     * maps the ID within any reference
     */
    protected void mapReference(Reference reference, Resource resource, UUID serviceId, UUID systemId) {
        if (reference == null) {
            return;
        }

        if (reference.hasReference()) {

            ReferenceComponents comps = ReferenceHelper.getReferenceComponents(reference);

            //if it's a reference to a patient resource, we perform an extra step to validate if the patient is known to us
            //if not, it still continues, but it will log the error
            if (comps.getResourceType() == ResourceType.Patient) {
                UUID patientEdsId = IdHelper.getEdsResourceId(serviceId, systemId, comps.getResourceType(), comps.getId());
                if (patientEdsId == null) {
                    LOG.error("Reference to unrecognised patient {} in {} {} for service {} and system {}",
                            comps.getId(),
                            resource.getResourceType(),
                            resource.getId(),
                            serviceId,
                            systemId);
                }
            }

            String newId = IdHelper.getOrCreateEdsResourceIdString(serviceId, systemId, comps.getResourceType(), comps.getId());
            reference.setReference(ReferenceHelper.createResourceReference(comps.getResourceType(), newId));

        } else {

            //if the reference doesn't have an actual reference, it will have an inline resource
            Resource referredResource = (Resource)reference.getResource();
            IdHelper.mapIds(serviceId, systemId, referredResource);
        }
    }

    /**
     * maps the ID within any reference
     */
    protected void mapReferences(List<Reference> references, Resource resource, UUID serviceId, UUID systemId) {
        if (references == null
                || references.isEmpty()) {
            return;
        }

        for (Reference reference: references) {
            mapReference(reference, resource, serviceId, systemId);
        }
    }

    public abstract void mapIds(Resource resource, UUID serviceId, UUID systemId);


}

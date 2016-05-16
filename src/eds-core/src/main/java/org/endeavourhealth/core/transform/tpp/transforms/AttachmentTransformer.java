package org.endeavourhealth.core.transform.tpp.transforms;

import org.endeavourhealth.core.transform.tpp.schema.Attachment;
import org.endeavourhealth.core.transform.tpp.schema.CarePlan;
import org.endeavourhealth.core.transform.tpp.schema.Event;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Resource;

import java.util.List;

public class AttachmentTransformer {

    public static void transform(List<Attachment> tppAttachments, Event tppEvent, Encounter fhirEncounter, List<Resource> fhirResources) {


    }
}

package org.endeavourhealth.core.transform.tpp.transforms;

import org.endeavourhealth.core.transform.tpp.schema.CarePlan;
import org.endeavourhealth.core.transform.tpp.schema.Event;
import org.endeavourhealth.core.transform.tpp.schema.Report;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Resource;

import java.util.List;

public class ReportTransformer {

    public static void transform(List<Report> tppReports, Event tppEvent, Encounter fhirEncounter, List<Resource> fhirResources) {


    }
}

package org.endeavourhealth.transform.emis.openhr.transforms;

import org.endeavourhealth.transform.fhir.ReferenceHelper;
import org.endeavourhealth.transform.common.TransformException;
import org.endeavourhealth.transform.emis.openhr.schema.OpenHR001HealthDomain;
import org.endeavourhealth.transform.emis.openhr.transforms.converters.CodeConverter;
import org.endeavourhealth.transform.emis.openhr.transforms.converters.DateConverter;
import org.endeavourhealth.transform.emis.openhr.transforms.helpers.EventEncounterMap;
import org.endeavourhealth.transform.fhir.FhirUris;
import org.hl7.fhir.instance.model.*;

public class ImmunisationTransformer implements ClinicalResourceTransformer
{
    public Immunization transform(OpenHR001HealthDomain.Event source, OpenHR001HealthDomain healthDomain, EventEncounterMap eventEncounterMap) throws TransformException
    {
        Immunization target = new Immunization();
        target.setId(source.getId());
        target.setMeta(new Meta().addProfile(FhirUris.PROFILE_URI_IMMUNIZATION));

        target.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED.toCode());
        target.setDateElement(DateConverter.convertPartialDateTimeToDateTimeType(source.getEffectiveTime()));
        target.setPatient(ReferenceHelper.createReference(ResourceType.Patient, source.getPatient()));
        target.setPerformer(ReferenceHelper.createReference(ResourceType.Practitioner, source.getAuthorisingUserInRole()));
        target.setEncounter(eventEncounterMap.getEncounterReference(source.getId()));
        target.setVaccineCode(CodeConverter.convertCode(source.getCode(), source.getDisplayTerm()));
        return target;
    }
}
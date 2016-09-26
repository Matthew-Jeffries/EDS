package org.endeavourhealth.transform.emis.csv.transforms.appointment;

import org.apache.commons.csv.CSVFormat;
import org.endeavourhealth.core.data.transform.ResourceIdMapRepository;
import org.endeavourhealth.core.data.transform.models.ResourceIdMapByEdsId;
import org.endeavourhealth.transform.common.CsvProcessor;
import org.endeavourhealth.transform.common.exceptions.FutureException;
import org.endeavourhealth.transform.common.exceptions.TransformException;
import org.endeavourhealth.transform.emis.csv.EmisCsvHelper;
import org.endeavourhealth.transform.emis.csv.schema.appointment.Session;
import org.endeavourhealth.transform.fhir.*;
import org.hl7.fhir.instance.model.*;

import java.util.Date;
import java.util.List;

public class SessionTransformer {

    public static void transform(String version,
                                 String folderPath,
                                 CSVFormat csvFormat,
                                 CsvProcessor csvProcessor,
                                 EmisCsvHelper csvHelper) throws Exception {

        Session parser = new Session(version, folderPath, csvFormat);
        try {
            while (parser.nextRecord()) {
                createSchedule(parser, csvProcessor, csvHelper);
            }
        } catch (FutureException fe) {
            throw fe;
        } catch (Exception ex) {
            throw new TransformException(parser.getErrorLine(), ex);
        } finally {
            parser.close();
        }
    }

    private static void createSchedule(Session sessionParser,
                                       CsvProcessor csvProcessor,
                                       EmisCsvHelper csvHelper) throws Exception {

        //skip deleted sessions
        if (sessionParser.getDeleted()) {
            return;
        }

        Schedule fhirSchedule = new Schedule();
        fhirSchedule.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_SCHEDULE));

        String sessionGuid = sessionParser.getAppointmnetSessionGuid();
        fhirSchedule.setId(sessionGuid);

        String locationGuid = sessionParser.getLocationGuid();
        Reference fhirReference = csvHelper.createLocationReference(locationGuid);
        fhirSchedule.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.SCHEDULE_LOCATION, fhirReference));

        Date start = sessionParser.getStartDateTime();
        Date end = sessionParser.getEndDateTime();
        Period fhirPeriod = PeriodHelper.createPeriod(start, end);
        fhirSchedule.setPlanningHorizon(fhirPeriod);

        String sessionType = sessionParser.getSessionTypeDescription();
        fhirSchedule.addType(CodeableConceptHelper.createCodeableConcept(sessionType));

        String category = sessionParser.getSessionCategoryDisplayName();
        fhirSchedule.setComment(category); //the FHIR description of "Comment" seems approproate to store the category

        String description = sessionParser.getDescription();
        fhirSchedule.setComment(description);

        List<String> userGuids = csvHelper.findSessionPractionersToSave(sessionGuid);

        //if we don't have any practitioners in the helper, then this may be a DELTA record from EMIS,
        //and we need to carry over the practitioners from our previous instance
        if (userGuids.isEmpty()) {
            try {
                Schedule fhirScheduleOld = (Schedule)csvHelper.retrieveResource(sessionGuid, ResourceType.Schedule, csvProcessor);

                ResourceIdMapRepository repository = new ResourceIdMapRepository();

                //then existing resource will have been through the mapping process, so we need to reverse-lookup the source
                //EMIS user GUID from the EDS ID
                String edsPractitionerId = ReferenceHelper.getReferenceId(fhirScheduleOld.getActor());
                ResourceIdMapByEdsId mapping = repository.getResourceIdMapByEdsId(ResourceType.Practitioner.toString(), edsPractitionerId);
                String emisUserGuid = mapping.getSourceId();
                userGuids.add(emisUserGuid);

                if (fhirScheduleOld.hasExtension()) {
                    for (Extension extension: fhirScheduleOld.getExtension()) {
                        if (extension.getUrl().equals(FhirExtensionUri.SCHEDULE_ADDITIONAL_ACTOR)) {
                            Reference oldAdditionalActor = (Reference)extension.getValue();
                            edsPractitionerId = ReferenceHelper.getReferenceId(oldAdditionalActor);
                            mapping = repository.getResourceIdMapByEdsId(ResourceType.Practitioner.toString(), edsPractitionerId);
                            emisUserGuid = mapping.getSourceId();
                            userGuids.add(emisUserGuid);
                        }
                    }
                }

            } catch (Exception ex) {
                //in production data, there should always be at least one practitioner for each session, but the
                //test data contains at least one session that doesn't, so we can end up here because we're
                //trying to find a previous instance of a resource that never existed before
            }
        }

        //add the user GUIDs to the FHIR resource
        if (!userGuids.isEmpty()) {

            //treat the first reference as the primary actor
            Reference first = ReferenceHelper.createReference(ResourceType.Practitioner, userGuids.get(0));
            fhirSchedule.setActor(first);

            //add any additional references as additional actors
            for (int i=1; i<userGuids.size(); i++) {
                Reference additional = ReferenceHelper.createReference(ResourceType.Practitioner, userGuids.get(i));
                fhirSchedule.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.SCHEDULE_ADDITIONAL_ACTOR, additional));
            }
        }

        csvProcessor.saveAdminResource(fhirSchedule);
    }
}

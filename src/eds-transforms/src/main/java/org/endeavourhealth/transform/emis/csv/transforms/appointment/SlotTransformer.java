package org.endeavourhealth.transform.emis.csv.transforms.appointment;

import org.apache.commons.csv.CSVFormat;
import org.endeavourhealth.transform.common.CsvProcessor;
import org.endeavourhealth.transform.emis.csv.EmisCsvHelper;
import org.endeavourhealth.transform.emis.csv.schema.Appointment_Slot;
import org.endeavourhealth.transform.fhir.FhirUri;
import org.hl7.fhir.instance.model.Appointment;
import org.hl7.fhir.instance.model.Meta;
import org.hl7.fhir.instance.model.Slot;

import java.util.Date;

public class SlotTransformer {

    public static void transform(String folderPath,
                                 CSVFormat csvFormat,
                                 CsvProcessor csvProcessor,
                                 EmisCsvHelper csvHelper) throws Exception {

        Appointment_Slot parser = new Appointment_Slot(folderPath, csvFormat);
        try {
            while (parser.nextRecord()) {
                createSlotAndAppointment(parser, csvProcessor, csvHelper);
            }
        } finally {
            parser.close();
        }
    }

    private static void createSlotAndAppointment(Appointment_Slot slotParser,
                                                 CsvProcessor csvProcessor,
                                                 EmisCsvHelper csvHelper) throws Exception {

        String patientGuid = slotParser.getPatientGuid();
        String organisationGuid = slotParser.getOrganisationGuid();

        Slot fhirSlot = new Slot();
        fhirSlot.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_SLOT));

        String slotGuid = slotParser.getSlotGuid();
        EmisCsvHelper.setUniqueId(fhirSlot, patientGuid, slotGuid);

        Appointment fhirAppointment = new Appointment();
        fhirAppointment.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_APPOINTMENT));

        //use the same slot GUID as the appointment GUID, since it's a different resource type, it should be fine
        EmisCsvHelper.setUniqueId(fhirAppointment, patientGuid, slotGuid);

        //if the Resource is to be deleted from the data store, then stop processing the CSV row
        if (slotParser.getDeleted()) {
            csvProcessor.deleteAdminResource(fhirSlot);
            csvProcessor.deletePatientResource(fhirAppointment, patientGuid);
            return;
        }

        String sessionGuid = slotParser.getSessionGuid();
        fhirSlot.setSchedule(csvHelper.createScheduleReference(sessionGuid));

        fhirSlot.setFreeBusyType(Slot.SlotStatus.BUSY);

        Date startDate = slotParser.getAppointmentStartDateTime();
        fhirSlot.setStart(startDate);

        //calculate expected end datetime from start, plus duration in mins
        long endMillis = startDate.getTime() + (slotParser.getPlannedDurationInMinutes() * 60 * 1000);
        Date endDate = new Date(endMillis);
        fhirSlot.setEnd(endDate);

        fhirAppointment.setStart(startDate);
        fhirAppointment.setEnd(new Date(endMillis));
        fhirAppointment.addSlot(csvHelper.createSlotReference(slotGuid));

        Appointment.AppointmentParticipantComponent fhirParticipant = fhirAppointment.addParticipant();
        fhirParticipant.setActor(csvHelper.createPatientReference(patientGuid));
        fhirParticipant.setStatus(Appointment.ParticipationStatus.ACCEPTED);

        if (slotParser.getDidNotAttend()) {
            fhirAppointment.setStatus(Appointment.AppointmentStatus.NOSHOW);
        } else if (slotParser.getLeftDateTime() != null) {
            fhirAppointment.setStatus(Appointment.AppointmentStatus.FULFILLED);
        } else if (slotParser.getSendInDateTime() != null) {
            fhirAppointment.setStatus(Appointment.AppointmentStatus.ARRIVED);
        } else {
            fhirAppointment.setStatus(Appointment.AppointmentStatus.BOOKED);
        }

        csvProcessor.saveAdminResource(fhirSlot);
        csvProcessor.savePatientResource(fhirAppointment, patientGuid);
    }
}
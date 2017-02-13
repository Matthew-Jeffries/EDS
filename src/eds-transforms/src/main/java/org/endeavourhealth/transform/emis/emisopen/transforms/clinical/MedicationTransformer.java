package org.endeavourhealth.transform.emis.emisopen.transforms.clinical;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.transform.common.exceptions.TransformException;
import org.endeavourhealth.transform.emis.emisopen.EmisOpenHelper;
import org.endeavourhealth.transform.emis.emisopen.schema.eommedicalrecord38.MedicalRecordType;
import org.endeavourhealth.transform.emis.emisopen.schema.eommedicalrecord38.MedicationListType;
import org.endeavourhealth.transform.emis.emisopen.schema.eommedicalrecord38.MedicationType;
import org.endeavourhealth.transform.emis.emisopen.transforms.common.CodeConverter;
import org.endeavourhealth.transform.emis.emisopen.transforms.common.DateConverter;
import org.endeavourhealth.transform.fhir.ExtensionConverter;
import org.endeavourhealth.transform.fhir.FhirExtensionUri;
import org.endeavourhealth.transform.fhir.FhirUri;
import org.hl7.fhir.instance.model.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public final class MedicationTransformer
{
    public static void transform(MedicalRecordType medicalRecordType, List<Resource> resources, String patientUuid) throws TransformException {

        MedicationListType medicationList = medicalRecordType.getMedicationList();
        if (medicationList == null) {
            return;
        }

        for (MedicationType medicationType : medicationList.getMedication()) {
            resources.add(transform(medicationType, patientUuid));
        }

    }

    public static MedicationStatement transform(MedicationType medicationType, String patientGuid) throws TransformException
    {
        MedicationStatement fhirMedicationStatement = new MedicationStatement();
        fhirMedicationStatement.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_MEDICATION_AUTHORISATION));

        EmisOpenHelper.setUniqueId(fhirMedicationStatement, patientGuid, medicationType.getGUID());

        fhirMedicationStatement.setPatient(EmisOpenHelper.createPatientReference(patientGuid));

        fhirMedicationStatement.setInformationSource(EmisOpenHelper.createPractitionerReference(medicationType.getAuthorisedUserID().getGUID()));
        fhirMedicationStatement.setDateAsserted(DateConverter.getDate(medicationType.getAssignedDate()));

        fhirMedicationStatement.setMedication(CodeConverter.convert(medicationType.getDrug().getPreparationID()));

        fhirMedicationStatement.addDosage(getDosage(medicationType));

        fhirMedicationStatement.addExtension(getQuantityExtension(medicationType));

        if (medicationType.getAuthorisedIssue() != null)
            fhirMedicationStatement.addExtension(getNumberOfRepeatsAllowedExtension(medicationType.getAuthorisedIssue()));

        if (medicationType.getIssueCount() != null)
            fhirMedicationStatement.addExtension(getNumberOfRepeatsIssuedExtension(medicationType.getIssueCount()));

        if (StringUtils.isNotBlank(medicationType.getPharmacyText()))
            fhirMedicationStatement.addExtension(getPharmacyTextExtension(medicationType.getPharmacyText()));

        if (StringUtils.isNotBlank(medicationType.getDateLastIssue()))
            fhirMedicationStatement.addExtension(getMostRecentIssueDateExtension(medicationType.getDateLastIssue()));

        if (medicationType.getContraceptiveIssue() != null)
            fhirMedicationStatement.addExtension(getPrescribedAsContraceptionExtension(medicationType.getContraceptiveIssue()));

        String expiry = medicationType.getDateRxExpire();
        boolean expired = false;
        if (!Strings.isNullOrEmpty(expiry)) {
            Date d = DateConverter.getDate(expiry);
            expired = d.before(new Date());

            //the cancellation extension is a compound extension, so we have one extension inside another
            Extension extension = ExtensionConverter.createExtension("date", new DateType(d));
            fhirMedicationStatement.addExtension(ExtensionConverter.createCompoundExtension(FhirExtensionUri.MEDICATION_AUTHORISATION_CANCELLATION, extension));
        }

        if (expired) {
            fhirMedicationStatement.setStatus(MedicationStatement.MedicationStatementStatus.ACTIVE);
        } else {
            fhirMedicationStatement.setStatus(MedicationStatement.MedicationStatementStatus.COMPLETED);
        }

        return fhirMedicationStatement;
    }

    private static MedicationStatement.MedicationStatementDosageComponent getDosage(MedicationType medicationType)
    {
        return new MedicationStatement.MedicationStatementDosageComponent()
                .setText(medicationType.getDosage());
    }

    private static Extension getQuantityExtension(MedicationType medicationType)
    {
        SimpleQuantity simpleQuantity = (SimpleQuantity)new SimpleQuantity()
                .setValue(BigDecimal.valueOf(medicationType.getQuantity()))
                .setUnit(medicationType.getQuantityUnits())
                .addExtension(new Extension()
                        .setUrl(FhirExtensionUri.QUANTITY_FREE_TEXT)
                        .setValue(new StringType(medicationType.getQuantityRepresentation())));

        return new Extension()
                .setUrl(FhirExtensionUri.MEDICATION_AUTHORISATION_QUANTITY)
                .setValue(simpleQuantity);
    }

    private static Extension getNumberOfRepeatsAllowedExtension(BigInteger authorisedIssue)
    {
        return new Extension()
                .setUrl(FhirExtensionUri.MEDICATION_AUTHORISATION_NUMBER_OF_REPEATS_ALLOWED)
                .setValue(new IntegerType(authorisedIssue.intValue()));
    }

    private static Extension getNumberOfRepeatsIssuedExtension(BigInteger issueCount)
    {
        return new Extension()
                .setUrl(FhirExtensionUri.MEDICATION_AUTHORISATION_NUMBER_OF_REPEATS_ISSUED)
                .setValue(new IntegerType(issueCount.intValue()));
    }

    private static Extension getPharmacyTextExtension(String pharmacyText)
    {
        return new Extension()
                .setUrl(FhirExtensionUri.PHARMACY_TEXT)
                .setValue(new StringType(pharmacyText));
    }

    private static Extension getMostRecentIssueDateExtension(String dateLastIssue) throws TransformException
    {
        return new Extension()
                .setUrl(FhirExtensionUri.MEDICATION_AUTHORISATION_MOST_RECENT_ISSUE_DATE)
                .setValue(new DateType(DateConverter.getDate(dateLastIssue)));
    }

    private static Extension getPrescribedAsContraceptionExtension(BigInteger contraceptiveIssue)
    {
        return new Extension()
                .setUrl(FhirExtensionUri.PRESCRIBED_AS_CONTRACEPTION)
                .setValue(new BooleanType((!contraceptiveIssue.equals(0))));
    }
}

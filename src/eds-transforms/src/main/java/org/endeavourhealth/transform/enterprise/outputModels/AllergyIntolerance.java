package org.endeavourhealth.transform.enterprise.outputModels;

import org.apache.commons.csv.CSVFormat;

import java.util.Date;

public class AllergyIntolerance extends AbstractEnterpriseCsvWriter {

    public AllergyIntolerance(String fileName, CSVFormat csvFormat, String dateFormat, String timeFormat) throws Exception {
        super(fileName, csvFormat, dateFormat, timeFormat);
    }

    public void writeDelete(int id) throws Exception {

        super.printRecord(OutputContainer.DELETE,
                "" + id);
    }

    public void writeUpsert(int id,
                            int organisationId,
                            int patientId,
                            Integer encounterId,
                            Integer practitionerId,
                            Date clinicalEffectiveDate,
                            Integer datePrecisionId,
                            Long snomedConceptId,
                            String originalCode,
                            String originalTerm) throws Exception {

        super.printRecord(OutputContainer.UPSERT,
                "" + id,
                "" + organisationId,
                "" + patientId,
                convertInt(encounterId),
                convertInt(practitionerId),
                convertDate(clinicalEffectiveDate),
                convertInt(datePrecisionId),
                convertLong(snomedConceptId),
                originalCode,
                originalTerm);
    }

    @Override
    public String[] getCsvHeaders() {
        return new String[] {
                "save_mode",
                "id",
                "organization_id",
                "patient_id",
                "encounter_id",
                "practitioner_id",
                "clinical_effective_date",
                "date_precision_id",
                "snomed_concept_id",
                "original_code",
                "original_term"
        };
    }

    @Override
    public Class[] getColumnTypes() {
        return new Class[] {
                String.class,
                Integer.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                Integer.class,
                Integer.class,
                Date.class,
                Integer.class,
                Long.class,
                String.class,
                String.class
        };
    }
}

package org.endeavourhealth.transform.emis.csv.transforms.careRecord;

import com.google.common.base.Strings;
import org.apache.commons.csv.CSVFormat;
import org.endeavourhealth.transform.common.CsvProcessor;
import org.endeavourhealth.transform.common.TransformException;
import org.endeavourhealth.transform.emis.csv.EmisDateTimeHelper;
import org.endeavourhealth.transform.emis.csv.schema.CareRecord_Problem;
import org.endeavourhealth.transform.emis.csv.EmisCsvHelper;
import org.endeavourhealth.transform.fhir.*;
import org.endeavourhealth.transform.fhir.schema.ProblemRelationshipType;
import org.endeavourhealth.transform.fhir.schema.ProblemSignificance;
import org.hl7.fhir.instance.model.*;

import java.util.Date;
import java.util.List;

public class ProblemTransformer {

    public static void transform(String folderPath, CSVFormat csvFormat, CsvProcessor csvProcessor, EmisCsvHelper csvHelper) throws Exception {

        CareRecord_Problem parser = new CareRecord_Problem(folderPath, csvFormat);
        try {
            while (parser.nextRecord()) {
                createProblem(parser, csvProcessor, csvHelper);
            }
        } finally {
            parser.close();
        }
    }

    private static void createProblem(CareRecord_Problem problemParser, CsvProcessor csvProcessor, EmisCsvHelper csvHelper) throws Exception {

        Condition fhirProblem = new Condition();
        fhirProblem.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_PROBLEM));

        String observationGuid = problemParser.getObservationGuid();
        String patientGuid = problemParser.getPatientGuid();
        String organisationGuid = problemParser.getOrganisationGuid();

        EmisCsvHelper.setUniqueId(fhirProblem, patientGuid, observationGuid);

        fhirProblem.setPatient(csvHelper.createPatientReference(patientGuid));

        String comments = problemParser.getComment();
        fhirProblem.setNotes(comments);

        Date endDate = problemParser.getEndDate();
        String endDatePrecision = problemParser.getEffectiveDatePrecision(); //NOTE; documentation refers to this as EffectiveDate, but this should be EndDate
        fhirProblem.setAbatement(EmisDateTimeHelper.createDateType(endDate, endDatePrecision));

        fhirProblem.setVerificationStatus(Condition.ConditionVerificationStatus.CONFIRMED);

        Integer expectedDuration = problemParser.getExpectedDuration();
        if (expectedDuration != null) {
            fhirProblem.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.PROBLEM_EXPECTED_DURATION, new IntegerType(expectedDuration.intValue())));
        }

        Date lastReviewDate = problemParser.getLastReviewDate();
        String lastReviewPrecision = problemParser.getLastReviewDatePrecision();
        DateType lastReviewDateType = EmisDateTimeHelper.createDateType(lastReviewDate, lastReviewPrecision);
        if (lastReviewDateType != null) {
            fhirProblem.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.PROBLEM_LAST_REVIEW_DATE, lastReviewDateType));
        }

        ProblemSignificance fhirSignificance = convertSignificance(problemParser.getSignificanceDescription());
        CodeableConcept fhirConcept = CodeableConceptHelper.createCodeableConcept(fhirSignificance);
        fhirProblem.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.PROBLEM_SIGNIFICANCE, fhirConcept));

        String parentProblemGuid = problemParser.getParentProblemObservationGuid();
        String parentRelationship = problemParser.getParentProblemRelationship();
        if (!Strings.isNullOrEmpty(parentProblemGuid)) {
            ProblemRelationshipType fhirRelationshipType = convertRelationshipType(parentRelationship);

            //this extension is composed of two separate extensions
            Extension typeExtension = ExtensionConverter.createExtension("type", new StringType(fhirRelationshipType.getCode()));
            Extension referenceExtension = ExtensionConverter.createExtension("target", csvHelper.createProblemReference(parentProblemGuid, patientGuid));
            fhirProblem.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.PROBLEM_RELATED, typeExtension, referenceExtension));
        }

        //until the Observation, DrugIssue and DrugRecord files are completed, we can't
        //save the problem, so cache it in the helper to finish later
        csvHelper.cacheProblem(patientGuid, observationGuid, fhirProblem);
    }

    private static ProblemRelationshipType convertRelationshipType(String relationshipType) throws Exception {

        if (relationshipType.equalsIgnoreCase("grouped")) {
            return ProblemRelationshipType.GROUPED;
        } else if (relationshipType.equalsIgnoreCase("combined")) {
            return ProblemRelationshipType.COMBINED;
        } else if (relationshipType.equalsIgnoreCase("evolved")) {
            return ProblemRelationshipType.EVOLVED_FROM;
        } else if (relationshipType.equalsIgnoreCase("replaced")) {
            return ProblemRelationshipType.REPLACES;
        } else {
            throw new TransformException("Unhanded problem relationship type " + relationshipType);
        }
    }

    private static ProblemSignificance convertSignificance(String significance) {
        significance = significance.toLowerCase();
        if (significance.indexOf("major") > -1) {
            return ProblemSignificance.SIGNIFICANT;
        } else if (significance.indexOf("minor") > -1) {
            return ProblemSignificance.NOT_SIGNIFICANT;
        } else {
            return ProblemSignificance.UNSPECIIED;
        }
    }


}

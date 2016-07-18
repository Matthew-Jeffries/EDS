package org.endeavourhealth.transform.emis;

import org.apache.commons.csv.CSVFormat;
import org.endeavourhealth.transform.common.CsvProcessor;
import org.endeavourhealth.transform.emis.csv.EmisCsvHelper;
import org.endeavourhealth.transform.emis.csv.transforms.admin.LocationTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.admin.OrganisationTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.admin.PatientTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.admin.UserInRoleTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.appointment.SessionTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.appointment.SlotTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.careRecord.*;
import org.endeavourhealth.transform.emis.csv.transforms.coding.ClinicalCodeTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.coding.DrugCodeTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.prescribing.DrugRecordTransformer;
import org.endeavourhealth.transform.emis.csv.transforms.prescribing.IssueRecordTransformer;

public abstract class EmisCsvTransformer {

    public static final String DATE_FORMAT = "yyyy-MM-dd"; //EMIS spec says "dd/MM/yyyy", but reality differs
    public static final String TIME_FORMAT = "hh:mm:ss";
    public static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT;

    public static void transform(String folderPath, CsvProcessor csvProcessor) throws Exception {

        EmisCsvHelper csvHelper = new EmisCsvHelper();

        transformCodes(folderPath, csvProcessor, csvHelper);
        transformAdminData(folderPath, csvProcessor, csvHelper);
        transformPatientData(folderPath, csvProcessor, csvHelper);

        //tell the processor we've completed all the files, so we can now start passing work to the protocols queue
        csvProcessor.processingCompleted();
    }

    private static void transformCodes(String folderPath, CsvProcessor csvProcessor, EmisCsvHelper csvHelper) throws Exception {

        ClinicalCodeTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        DrugCodeTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
    }

    private static void transformAdminData(String folderPath, CsvProcessor csvProcessor, EmisCsvHelper csvHelper) throws Exception {

        LocationTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        OrganisationTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        UserInRoleTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        SessionTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
    }

    private static void transformPatientData(String folderPath, CsvProcessor csvProcessor, EmisCsvHelper csvHelper) throws Exception {

        //invoke any pre-transformers, which extract referential data from the files before the main transforms
        ObservationPreTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);

        //note the order of these transforms is important, as consultations should be before obs etc.
        PatientTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        ConsultationTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        ObservationReferralTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        ProblemTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        ObservationTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        DrugRecordTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        IssueRecordTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        SlotTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);
        DiaryTransformer.transform(folderPath, CSV_FORMAT, csvProcessor, csvHelper);

        //if we have any new Obs that reference pre-existing parent obs or problems,
        //then we need to retrieve the existing resources and update them
        csvHelper.processRemainingObservationParentChildLinks(csvProcessor);
        csvHelper.processRemainingProblemRelationships(csvProcessor);
    }



}

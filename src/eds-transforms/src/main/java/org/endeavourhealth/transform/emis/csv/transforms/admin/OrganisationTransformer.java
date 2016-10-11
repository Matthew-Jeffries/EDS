package org.endeavourhealth.transform.emis.csv.transforms.admin;

import com.google.common.base.Strings;
import org.endeavourhealth.transform.common.CsvProcessor;
import org.endeavourhealth.transform.emis.csv.EmisCsvHelper;
import org.endeavourhealth.transform.emis.csv.schema.AbstractCsvTransformer;
import org.endeavourhealth.transform.emis.csv.schema.admin.Organisation;
import org.endeavourhealth.transform.fhir.*;
import org.endeavourhealth.transform.fhir.schema.OrganisationType;
import org.hl7.fhir.instance.model.*;

import java.util.Date;
import java.util.Map;

public class OrganisationTransformer {

    public static void transform(String version,
                                 Map<Class, AbstractCsvTransformer> parsers,
                                 CsvProcessor csvProcessor,
                                 EmisCsvHelper csvHelper) throws Exception {

        Organisation parser = (Organisation)parsers.get(Organisation.class);

        while (parser.nextRecord()) {

            try {
                createResource(parser, csvProcessor, csvHelper);
            } catch (Exception ex) {
                csvProcessor.logTransformRecordError(ex, parser.getCurrentState());
            }

        }
    }

    private static void createResource(Organisation parser,
                                       CsvProcessor csvProcessor,
                                       EmisCsvHelper csvHelper) throws Exception {

        Organization fhirOrganisation = new Organization();
        fhirOrganisation.setMeta(new Meta().addProfile(FhirUri.PROFILE_URI_ORGANIZATION));

        String orgGuid = parser.getOrganisationGuid();
        fhirOrganisation.setId(orgGuid);

        String odsCode = parser.getODScode();
        Identifier fhirIdentifier = IdentifierHelper.createOdsOrganisationIdentifier(odsCode);
        fhirOrganisation.addIdentifier(fhirIdentifier);

        String name = parser.getOrganisatioName();
        fhirOrganisation.setName(name);

        Date openDate = parser.getOpenDate();
        Date closeDate = parser.getCloseDate();
        Period fhirPeriod = PeriodHelper.createPeriod(openDate, closeDate);
        fhirOrganisation.setActive(PeriodHelper.isActive(fhirPeriod));
        fhirOrganisation.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.ACTIVE_PERIOD, fhirPeriod));

        String parentOrganisationGuid = parser.getParentOrganisationGuid();
        if (!Strings.isNullOrEmpty(parentOrganisationGuid)) {
            fhirOrganisation.setPartOf(csvHelper.createOrganisationReference(parentOrganisationGuid));
        }

        String ccgOrganisationGuid = parser.getCCGOrganisationGuid();
        if (!Strings.isNullOrEmpty(ccgOrganisationGuid)) {
            fhirOrganisation.setPartOf(csvHelper.createOrganisationReference(ccgOrganisationGuid));
        }

        String organisationType = parser.getOrganisationType();
        OrganisationType fhirOrgType = convertOrganisationType(organisationType);
        if (fhirOrgType != null) {
            fhirOrganisation.setType(CodeableConceptHelper.createCodeableConcept(fhirOrgType));
        } else {
            //if the org type from the CSV can't be mapped to one of the value set, store as a freetext type
            fhirOrganisation.setType(CodeableConceptHelper.createCodeableConcept(organisationType));
        }

        String mainLocationGuid = parser.getMainLocationGuid();
        Reference fhirReference = csvHelper.createLocationReference(mainLocationGuid);
        fhirOrganisation.addExtension(ExtensionConverter.createExtension(FhirExtensionUri.ORGANISATION_MAIN_LOCATION, fhirReference));

        csvProcessor.saveAdminResource(parser.getCurrentState(), fhirOrganisation);
    }

    private static OrganisationType convertOrganisationType(String csvOrganisationType) {
        try {
            return OrganisationType.fromDescription(csvOrganisationType);
        } catch (Exception ex) {

            //the below mappings are based on what was present in the EMIS CSV sample files
            //EMIS has been asked for a complete list, but until this is made available, these
            //are the only known types. There are a number of organisation types, such as "Hospice"
            //or "Community" which don't map to any official NHS organisation type
            if (csvOrganisationType.equalsIgnoreCase("General Practice")
                || csvOrganisationType.equalsIgnoreCase("General Practice Surgery")
                || csvOrganisationType.equalsIgnoreCase("Main Surgery")) {
                return OrganisationType.GP_PRACTICE;

            } else if (csvOrganisationType.equalsIgnoreCase("CCG")) {
                return OrganisationType.CCG;

            } else if (csvOrganisationType.equalsIgnoreCase("PCT Site")
                    || csvOrganisationType.equalsIgnoreCase("Primary Care Trust")) {
                return OrganisationType.PCT;

            } else if (csvOrganisationType.equalsIgnoreCase("Hospital")
                    || csvOrganisationType.equalsIgnoreCase("NHS Trust Site")
                    || csvOrganisationType.equalsIgnoreCase("NHS Trust")) {
                return OrganisationType.NHS_TRUST;

            } else {
                return null;
            }
        }

    }
}
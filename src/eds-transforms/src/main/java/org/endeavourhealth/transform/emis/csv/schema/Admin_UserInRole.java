package org.endeavourhealth.transform.emis.csv.schema;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.endeavourhealth.transform.common.TransformException;
import org.endeavourhealth.transform.emis.EmisCsvTransformer;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class Admin_UserInRole extends AbstractCsvTransformer {

    public Admin_UserInRole(String folderPath, CSVFormat csvFormat) throws Exception {
        super(folderPath, csvFormat, EmisCsvTransformer.DATE_FORMAT, EmisCsvTransformer.TIME_FORMAT);
    }

    @Override
    protected String[] getCsvHeaders() {
        return new String[]{
                "UserInRoleGuid",
                "OrganisationGuid",
                "Title",
                "GivenName",
                "Surname",
                "JobCategoryCode",
                "JobCategoryName",
                "ContractStartDate",
                "ContractEndDate",
                "ProcessingId"
        };
    }

    public String getUserInRoleGuid() {
        return super.getString(0);
    }
    public String getOrganisationGuid() {
        return super.getString(1);
    }
    public String getTitle() {
        return super.getString(2);
    }
    public String getGivenName() {
        return super.getString(3);
    }
    public String getSurname() {
        return super.getString(4);
    }
    public String getJobCategoryCode() {
        return super.getString(5);
    }
    public String getJobCategoryName() {
        return super.getString(6);
    }
    public Date getContractStartDate() throws TransformException {
        return super.getDate(7);
    }
    public Date getContractEndDate() throws TransformException {
        return super.getDate(8);
    }
    public Integer getProcessingId() {
        return super.getInt(9);
    }
}

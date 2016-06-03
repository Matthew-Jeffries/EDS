package org.endeavourhealth.ui.utility;

import org.endeavourhealth.ui.querydocument.AbstractQueryDocumentReader;
import org.endeavourhealth.ui.querydocument.models.*;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class QueryDocumentReaderFindDependentUuids extends AbstractQueryDocumentReader {

    private HashSet<UUID> uuids = new HashSet<UUID>();

    public QueryDocumentReaderFindDependentUuids(QueryDocument doc) {
        super(doc);
    }

    public HashSet<UUID> findUuids()
    {
        super.processQueryDocument();

        return uuids;
    }

    private void addUuid(String uuid) {
        if (uuid != null && !uuid.isEmpty()) {
            uuids.add(UUID.fromString(uuid));
        }
    }

    @Override
    protected void processQuery(Query query) {
        addUuid(query.getParentQueryUuid());

        super.processQuery(query);
    }

    @Override
    protected void processReportItem(ReportItem reportItem) {
        addUuid(reportItem.getListReportLibraryItemUuid());
        addUuid(reportItem.getQueryLibraryItemUuid());

        super.processReportItem(reportItem);
    }

    @Override
    protected void processRule(Rule rule) {
        addUuid(rule.getQueryLibraryItemUUID());
        addUuid(rule.getTestLibraryItemUUID());

        super.processRule(rule);
    }

    @Override
    protected void processDataSource(DataSource dataSource) {
        List<String> ids = dataSource.getDataSourceUuid();
        for (String id: ids) {
            addUuid(id);
        }

        super.processDataSource(dataSource);
    }

    @Override
    protected void processCalculationParameter(CalculationParameter calculationParameter) {
        addUuid(calculationParameter.getDataSourceUuid());

        super.processCalculationParameter(calculationParameter);
    }

    @Override
    protected void processTest(Test test) {
        addUuid(test.getDataSourceUuid());

        super.processTest(test);
    }

    @Override
    protected void processFieldTest(FieldTest fieldTest) {
        List<String> ids = fieldTest.getCodeSetLibraryItemUuid();
        for (String id: ids) {
            addUuid(id);
        }

        super.processFieldTest(fieldTest);
    }
}

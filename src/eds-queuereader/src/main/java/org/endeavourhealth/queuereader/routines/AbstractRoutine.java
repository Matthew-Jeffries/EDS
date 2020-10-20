package org.endeavourhealth.queuereader.routines;

import com.google.common.base.Strings;
import org.endeavourhealth.core.database.dal.admin.models.Service;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * common functions used by lots of one-off routines
 */
public abstract class AbstractRoutine {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRoutine.class);

    private static final int BULK_STARTED = 0;
    private static final int BULK_DONE = 1;

    public static boolean shouldSkipService(Service service, String odsCodeRegex) {
        if (Strings.isNullOrEmpty(odsCodeRegex)) {
            return false;
        }

        String odsCode = service.getLocalId();
        if (!Strings.isNullOrEmpty(odsCode)
                && Pattern.matches(odsCodeRegex, odsCode)) {
            return false;
        }

        String ccgCode = service.getCcgCode();
        if (!Strings.isNullOrEmpty(ccgCode)
                && Pattern.matches(odsCodeRegex, ccgCode)) {
            return false;
        }

        LOG.debug("Skipping " + service + " due to regex");
        return true;
    }


    /**
     * checks if the given service has already done the given bulk operation and audits the start if not
     */
    public static boolean isServiceDoneBulkOperation(Service service, String bulkOperationName) throws Exception {

        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT 1"
                    + " FROM bulk_operation_audit"
                    + " WHERE service_id = ?"
                    + " AND operation_name = ?"
                    + " AND status = ?";
            ps = connection.prepareStatement(sql);
            int col = 1;
            ps.setString(col++, service.getId().toString());
            ps.setString(col++, bulkOperationName);
            ps.setInt(col++, BULK_DONE);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }

        startBulkOperation(service, bulkOperationName);
        return false;
    }

    public static boolean isServiceStartedOrDoneBulkOperation(Service service, String bulkOperationName) throws Exception {

        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "SELECT 1"
                    + " FROM bulk_operation_audit"
                    + " WHERE service_id = ?"
                    + " AND operation_name = ?"
                    + " AND (status = ? OR status = ?)";
            ps = connection.prepareStatement(sql);
            int col = 1;
            ps.setString(col++, service.getId().toString());
            ps.setString(col++, bulkOperationName);
            ps.setInt(col++, BULK_STARTED);
            ps.setInt(col++, BULK_DONE);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }

        startBulkOperation(service, bulkOperationName);
        return false;
    }

    private static void startBulkOperation(Service service, String bulkOperationName) throws Exception {

        //if not done, audit that we're doing it
        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "INSERT INTO bulk_operation_audit (service_id, operation_name, status, started) "
                    + " VALUES (?, ?, ?, ?)";
            ps = connection.prepareStatement(sql);
            int col = 1;
            ps.setString(col++, service.getId().toString());
            ps.setString(col++, bulkOperationName);
            ps.setInt(col++, BULK_STARTED); //0 = started
            ps.setTimestamp(col++, new java.sql.Timestamp(new Date().getTime()));
            ps.executeUpdate();
            connection.commit();

        } catch (Exception ex) {
            connection.rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    /**
     * updates the bulk operation audit table to say the given bulk is done
     */
    public static void setServiceDoneBulkOperation(Service service, String bulkOperationName) throws Exception {

        Connection connection = ConnectionManager.getAuditConnection();
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE bulk_operation_audit "
                    + " SET status = ?, finished = ? "
                    + "WHERE service_id = ? "
                    + "AND operation_name = ? "
                    + "AND status = ?";
            ps = connection.prepareStatement(sql);
            int col = 1;
            ps.setInt(col++, BULK_DONE);
            ps.setTimestamp(col++, new java.sql.Timestamp(new Date().getTime()));
            ps.setString(col++, service.getId().toString());
            ps.setString(col++, bulkOperationName);
            ps.setInt(col++, BULK_STARTED);
            ps.executeUpdate();
            connection.commit();

        } catch (Exception ex) {
            connection.rollback();
            throw ex;

        } finally {
            if (ps != null) {
                ps.close();
            }
            connection.close();
        }
    }

    /**
     * handy fn to stop a routine for manual inspection before continuing (or quitting)
     */
    public static void continueOrQuit() throws Exception {
        LOG.info("Enter y to continue, anything else to quit");

        byte[] bytes = new byte[10];
        java.lang.System.in.read(bytes);
        char c = (char) bytes[0];
        if (c != 'y' && c != 'Y') {
            java.lang.System.out.println("Read " + c);
            java.lang.System.exit(1);
        }
    }


}
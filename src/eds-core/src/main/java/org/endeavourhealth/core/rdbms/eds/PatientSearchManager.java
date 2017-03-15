package org.endeavourhealth.core.rdbms.eds;

import com.fasterxml.jackson.databind.JsonNode;
import org.endeavourhealth.common.config.ConfigManager;
import org.endeavourhealth.core.fhirStorage.metadata.ReferenceHelper;
import org.hl7.fhir.instance.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.*;

public class PatientSearchManager {
    private static final Logger LOG = LoggerFactory.getLogger(PatientSearchManager.class);

    private final static String IDENTIFIER_SYSTEM_NHSNUMBER = "http://fhir.nhs.net/Id/nhs-number";

    private static EntityManagerFactory entityManager;


    public static void update(UUID serviceId, UUID systemId, Patient fhirPatient) throws Exception {
        update(serviceId, systemId, fhirPatient, null);
    }

    public static void update(UUID serviceId, UUID systemId, EpisodeOfCare fhirEpisode) throws Exception {
        update(serviceId, systemId, null, fhirEpisode);
    }

    private static void update(UUID serviceId, UUID systemId, Patient fhirPatient, EpisodeOfCare fhirEpisode) throws Exception {

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        PatientSearch patientSearch = createOrUpdatePatientSearch(serviceId, systemId, fhirPatient, fhirEpisode, entityManager);
        entityManager.persist(patientSearch);

        if (fhirPatient != null) {
            List<PatientSearchLocalIdentifier> localIdentifiers = createOrUpdateLocalIdentifiers(serviceId, systemId, fhirPatient, entityManager);
            for (PatientSearchLocalIdentifier localIdentifier: localIdentifiers) {
                entityManager.persist(localIdentifier);
            }
        }

        //entityManager.merge(patientSearch);

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    private static List<PatientSearchLocalIdentifier> createOrUpdateLocalIdentifiers(UUID serviceId, UUID systemId, Patient fhirPatient, EntityManager entityManager) {
        String patientId = findPatientId(fhirPatient, null);

        String sql = "select c"
                + " from "
                + " PatientSearchLocalIdentifier c"
                + " where c.serviceId = :service_id"
                + " and c.systemId = :system_id"
                + " and c.patientId = :patient_id";

        Query query = entityManager.createQuery(sql, PatientSearchLocalIdentifier.class)
                .setParameter("service_id", serviceId.toString())
                .setParameter("system_id", systemId.toString())
                .setParameter("patient_id", patientId);

        List<PatientSearchLocalIdentifier> list = query.getResultList();

        Map<String, PatientSearchLocalIdentifier> existingMap = new HashMap<>();
        for (PatientSearchLocalIdentifier localIdentifier: list) {
            String system = localIdentifier.getLocalIdSystem();
            existingMap.put(system, localIdentifier);
        }

        if (fhirPatient.hasIdentifier()) {
            for (Identifier fhirIdentifier : fhirPatient.getIdentifier()) {

                if (!fhirIdentifier.getSystem().equalsIgnoreCase(IDENTIFIER_SYSTEM_NHSNUMBER)) {
                    String system = fhirIdentifier.getSystem();
                    String value = fhirIdentifier.getValue();

                    PatientSearchLocalIdentifier localIdentifier = existingMap.get(system);
                    if (localIdentifier == null) {
                        localIdentifier = new PatientSearchLocalIdentifier();
                        localIdentifier.setServiceId(serviceId.toString());
                        localIdentifier.setSystemId(systemId.toString());
                        localIdentifier.setPatientId(patientId);
                        localIdentifier.setLocalIdSystem(system);

                        list.add(localIdentifier);
                    }

                    localIdentifier.setLocalId(value);
                    localIdentifier.setLastUpdated(new Date());
                }
            }
        }

        return list;
    }

    private static PatientSearch createOrUpdatePatientSearch(UUID serviceId, UUID systemId, Patient fhirPatient, EpisodeOfCare fhirEpisode, EntityManager entityManager) throws Exception {
        String patientId = findPatientId(fhirPatient, fhirEpisode);

        String sql = "select c"
                + " from"
                + " PatientSearch c"
                + " where c.serviceId = :serviceId"
                + " and c.systemId = :systemId"
                + " and c.patientId = :patientId";

        Query query = entityManager.createQuery(sql, PatientSearch.class)
                .setParameter("serviceId", serviceId.toString())
                .setParameter("systemId", systemId.toString())
                .setParameter("patientId", patientId);

        PatientSearch patientSearch = null;
        try {
            patientSearch = (PatientSearch)query.getSingleResult();

        } catch (NoResultException ex) {
            patientSearch = new PatientSearch();
            patientSearch.setServiceId(serviceId.toString());
            patientSearch.setSystemId(systemId.toString());
            patientSearch.setPatientId(patientId);
        }

        if (fhirPatient != null) {

            String nhsNumber = findNhsNumber(fhirPatient);
            String forenames = findForenames(fhirPatient);
            String surname = findSurname(fhirPatient);
            String postcode = findPostcode(fhirPatient);
            String gender = findGender(fhirPatient);
            Date dob = fhirPatient.getBirthDate();
            Date dod = findDateOfDeath(fhirPatient);

            patientSearch.setNhsNumber(nhsNumber);
            patientSearch.setForenames(forenames);
            patientSearch.setSurname(surname);
            patientSearch.setPostcode(postcode);
            patientSearch.setGender(gender);
            patientSearch.setDateOfBirth(dob);
            patientSearch.setDateOfDeath(dod);
        }

        if (fhirEpisode != null) {

            Date regStart = null;
            Date regEnd = null;
            if (fhirEpisode.hasPeriod()) {
                Period period = fhirEpisode.getPeriod();
                if (period.hasStart()) {
                    regStart = period.getStart();
                }
                if (period.hasEnd()) {
                    regEnd = period.getEnd();
                }
            }

            patientSearch.setRegistrationStart(regStart);
            patientSearch.setRegistrationEnd(regEnd);
        }

        patientSearch.setLastUpdated(new Date());

        return patientSearch;
    }

    private static String findGender(Patient fhirPatient) {
        if (fhirPatient.hasGender()) {
            return fhirPatient.getGender().getDisplay();
        } else {
            return null;
        }
    }

    private static Date findDateOfDeath(Patient fhirPatient) throws Exception {
        if (fhirPatient.hasDeceasedDateTimeType()) {
            return fhirPatient.getDeceasedDateTimeType().getValue();
        } else {
            return null;
        }
    }


    private static String findForenames(Patient fhirPatient) {
        List<String> forenames = new ArrayList<>();

        for (HumanName fhirName: fhirPatient.getName()) {
            if (fhirName.getUse() != HumanName.NameUse.OFFICIAL) {
                continue;
            }

            for (StringType given: fhirName.getGiven()) {
                forenames.add(given.getValue());
            }
        }
        return String.join(" ", forenames);
    }

    private static String findSurname(Patient fhirPatient) {
        List<String> surnames = new ArrayList<>();

        for (HumanName fhirName: fhirPatient.getName()) {
            if (fhirName.getUse() != HumanName.NameUse.OFFICIAL) {
                continue;
            }

            for (StringType family: fhirName.getFamily()) {
                surnames.add(family.getValue());
            }
        }
        return String.join(" ", surnames);
    }

    private static String findPostcode(Patient fhirPatient) {

        for (Address fhirAddress: fhirPatient.getAddress()) {
            if (fhirAddress.getUse() != Address.AddressUse.HOME) {
                continue;
            }
            return fhirAddress.getPostalCode();
        }
        return null;
    }

    private static String findNhsNumber(Patient fhirPatient) {

        for (Identifier fhirIdentifier: fhirPatient.getIdentifier()) {
            if (fhirIdentifier.getSystem().equals(IDENTIFIER_SYSTEM_NHSNUMBER)) {
                String val = fhirIdentifier.getValue();
                val = val.replace(" ", "");
                if (val.length() == 10) {
                    return val;
                }
            }
        }
        return null;
    }

    private static String findPatientId(Patient fhirPatient, EpisodeOfCare fhirEpisode) {
        if (fhirPatient != null) {
            return fhirPatient.getId();

        } else {
            Reference reference = fhirEpisode.getPatient();
            return ReferenceHelper.getReferenceId(reference);
        }
    }

    private static EntityManager getEntityManager() throws Exception {

        if (entityManager == null
                || !entityManager.isOpen()) {
            createEntityManager();
        }

        return entityManager.createEntityManager();
    }

    private static synchronized void createEntityManager() throws Exception {

        JsonNode json = ConfigManager.getConfigurationAsJson("eds_db");
        String url = json.get("url").asText();
        String user = json.get("username").asText();
        String pass = json.get("password").asText();

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
        properties.put("hibernate.hikari.dataSource.url", url);
        properties.put("hibernate.hikari.dataSource.user", user);
        properties.put("hibernate.hikari.dataSource.password", pass);

        entityManager = Persistence.createEntityManagerFactory("EdsDb", properties);
    }

    public static void delete(UUID serviceId, UUID systemId) throws Exception {

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        String sql = "delete"
                + " from"
                + " PatientSearchLocalIdentifier c"
                + " where c.serviceId = :serviceId"
                + " and c.systemId = :systemId";

        Query query = entityManager.createQuery(sql)
                .setParameter("serviceId", serviceId.toString())
                .setParameter("systemId", systemId.toString());
        query.executeUpdate();

        sql = "delete"
                + " from"
                + " PatientSearch c"
                + " where c.serviceId = :serviceId"
                + " and c.systemId = :systemId";

        query = entityManager.createQuery(sql)
                .setParameter("serviceId", serviceId.toString())
                .setParameter("systemId", systemId.toString());
        query.executeUpdate();

        entityManager.getTransaction().commit();
    }

    public static List<PatientSearch> searchByNhsNumber(String nhsNumber) throws Exception {
        EntityManager entityManager = getEntityManager();

        String sql = "select c"
                + " from"
                + " PatientSearch c"
                + " where c.nhsNumber = :nhs_number";

        Query query = entityManager.createQuery(sql, PatientSearch.class)
                .setParameter("nhs_number", nhsNumber);

        List<PatientSearch> results = query.getResultList();
        entityManager.close();
        return results;
    }

    public static List<PatientSearch> searchByLocalId(UUID serviceId, UUID systemId, String localId) throws Exception {
        EntityManager entityManager = getEntityManager();

        String sql = "select c"
                + " from"
                + " PatientSearch c"
                + " inner join PatientSearchLocalIdentifier l"
                + " on c.serviceId = l.serviceId"
                + " and c.systemId = l.systemId"
                + " and c.patientId = l.patientId"
                + " where l.localId = :localId"
                + " and l.serviceId = :serviceId"
                + " and l.systemId = :systemId";

        Query query = entityManager.createQuery(sql, PatientSearch.class)
                .setParameter("localId", localId)
                .setParameter("serviceId", serviceId.toString())
                .setParameter("systemId", systemId.toString());

        List<PatientSearch> results = query.getResultList();
        entityManager.close();
        return results;
    }

    public static List<PatientSearch> searchByDateOfBirth(UUID serviceId, UUID systemId, Date dateOfBirth) throws Exception {
        EntityManager entityManager = getEntityManager();

        String sql = "select c"
                + " from"
                + " PatientSearch c"
                + " where c.dateOfBirth = :dateOfBirth"
                + " and c.serviceId = :serviceId"
                + " and c.systemId = :systemId";

        Query query = entityManager.createQuery(sql, PatientSearch.class)
                .setParameter("dateOfBirth", dateOfBirth)
                .setParameter("serviceId", serviceId.toString())
                .setParameter("systemId", systemId.toString());

        List<PatientSearch> results = query.getResultList();
        entityManager.close();
        return results;
    }

    public static List<PatientSearch> searchByNhsNumber(UUID serviceId, UUID systemId, String nhsNumber) throws Exception {
        EntityManager entityManager = getEntityManager();

        String sql = "select c"
                + " from"
                + " PatientSearch c"
                + " where c.nhsNumber = :nhs_number"
                + " and c.serviceId = :serviceId"
                + " and c.systemId = :systemId";

        Query query = entityManager.createQuery(sql, PatientSearch.class)
                .setParameter("nhs_number", nhsNumber)
                .setParameter("serviceId", serviceId.toString())
                .setParameter("systemId", systemId.toString());

        List<PatientSearch> results = query.getResultList();
        entityManager.close();
        return results;
    }

    public static List<PatientSearch> searchByNames(UUID serviceId, UUID systemId, List<String> names) throws Exception {

        if (names.isEmpty()) {
            throw new IllegalArgumentException("Names cannot be empty");
        }

        EntityManager entityManager = getEntityManager();

        List<PatientSearch> results = null;

        //if just one name, then treat as a surname
        if (names.size() == 1) {

            String surname = names.get(0) + "%";

            String sql = "select c"
                    + " from"
                    + " PatientSearch c"
                    + " where lower(c.surname) LIKE lower(:surname)"
                    + " and c.serviceId = :serviceId"
                    + " and c.systemId = :systemId";

            Query query = entityManager.createQuery(sql, PatientSearch.class)
                    .setParameter("surname", surname)
                    .setParameter("serviceId", serviceId.toString())
                    .setParameter("systemId", systemId.toString());

            results = query.getResultList();

        } else {

            //if multiple tokens, then treat all but the last as forenames
            names = new ArrayList(names);
            String surname = names.remove(names.size()-1) + "%";
            String forenames = String.join("% ", names) + "%";

            String sql = "select c"
                    + " from"
                    + " PatientSearch c"
                    + " where lower(c.surname) LIKE lower(:surname)"
                    + " and lower(c.forenames) LIKE lower(:forenames)"
                    + " and c.serviceId = :serviceId"
                    + " and c.systemId = :systemId";

            Query query = entityManager.createQuery(sql, PatientSearch.class)
                    .setParameter("surname", surname)
                    .setParameter("forenames", forenames)
                    .setParameter("serviceId", serviceId.toString())
                    .setParameter("systemId", systemId.toString());

            results = query.getResultList();
        }

        entityManager.close();
        return results;
    }

    public static PatientSearch searchByPatientId(UUID patientId) throws Exception {
        EntityManager entityManager = getEntityManager();

        String sql = "select c"
                + " from"
                + " PatientSearch c"
                + " where c.patientId = :patientId";

        Query query = entityManager.createQuery(sql, PatientSearch.class)
                .setParameter("patientId", patientId.toString());

        try {
            return (PatientSearch)query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }


}

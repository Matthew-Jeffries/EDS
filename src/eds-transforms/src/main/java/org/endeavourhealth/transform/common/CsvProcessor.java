package org.endeavourhealth.transform.common;

import com.datastax.driver.core.utils.UUIDs;
import org.endeavourhealth.core.data.ehr.ExchangeBatchRepository;
import org.endeavourhealth.core.data.ehr.models.ExchangeBatch;
import org.endeavourhealth.core.fhirStorage.FhirStorageService;
import org.endeavourhealth.transform.common.exceptions.PatientResourceException;
import org.endeavourhealth.transform.common.exceptions.TransformException;
import org.hl7.fhir.instance.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CsvProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CsvProcessor.class);

    private static final int THREAD_POOL_SIZE = 10; //arbitrary choice

    private static Set<Class> patientResourceClasses = null;

    private final UUID exchangeId;
    private final UUID serviceId;
    private final UUID systemId;
    private final FhirStorageService storageService;
    private final ExchangeBatchRepository exchangeBatchRepository;

    //batch IDs
    private ReentrantLock batchIdLock = new ReentrantLock();
    private Map<String, UUID> patientBatchIdMap = new ConcurrentHashMap<>();
    private UUID adminBatchId = null;

    //threading
    private ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private AtomicInteger threadPoolQueueSize = new AtomicInteger(); //executorService doesn't provide visibility of this, so keep separate
    private ReentrantLock futuresLock = new ReentrantLock();
    private Map<Future, Future> futures = new ConcurrentHashMap<>(); //need concurrency support, so can't use a set

    //counts
    private Map<UUID, AtomicInteger> countResourcesSaved = new ConcurrentHashMap<>();
    private Map<UUID, AtomicInteger> countResourcesDeleted = new ConcurrentHashMap<>();


    public CsvProcessor(UUID exchangeId, UUID serviceId, UUID systemId) {
        this.exchangeId = exchangeId;
        this.serviceId = serviceId;
        this.systemId = systemId;
        this.storageService = new FhirStorageService(serviceId, systemId);
        this.exchangeBatchRepository = new ExchangeBatchRepository();
    }


    public void saveAdminResource(Resource resource) throws Exception {
        saveAdminResource(resource, true);
    }
    public void saveAdminResource(Resource resource, boolean mapIds) throws Exception {
        addResourceToQueue(resource, false, mapIds, getAdminBatchId(), false);
    }

    public void deleteAdminResource(Resource resource) throws Exception {
        deleteAdminResource(resource, true);
    }
    public void deleteAdminResource(Resource resource, boolean mapIds) throws Exception {
        addResourceToQueue(resource, false, mapIds, getAdminBatchId(), true);
    }

    public void savePatientResource(Resource resource, String patientId) throws Exception {
        savePatientResource(resource, true, patientId);
    }
    public void savePatientResource(Resource resource, boolean mapIds, String patientId) throws Exception {
        addResourceToQueue(resource, true, mapIds, getPatientBatchId(patientId), false);
    }

    public void deletePatientResource(Resource resource, String patientId) throws Exception {
        deletePatientResource(resource, true, patientId);
    }
    public void deletePatientResource(Resource resource, boolean mapIds, String patientId) throws Exception {
        addResourceToQueue(resource, true, mapIds, getPatientBatchId(patientId), true);
    }

    private void addResourceToQueue(Resource resource,
                                    boolean expectingPatientResource,
                                    boolean mapIds,
                                    UUID batchId,
                                    boolean toDelete) throws Exception {

        //validate we're treating the resoure properly as admin / patient
        if (isPatientResource(resource) != expectingPatientResource) {
            throw new PatientResourceException(resource.getResourceType(), expectingPatientResource);
        }

        //increment our counters for auditing
        if (toDelete) {
            countResourcesDeleted.get(batchId).incrementAndGet();
        } else {
            countResourcesSaved.get(batchId).incrementAndGet();
        }


        //new WorkerCallable(resource, batchId, toDelete, mapIds).call();

        threadPoolQueueSize.incrementAndGet();
        Future future = threadPool.submit(new MapAndSaveResourceTask(resource, batchId, toDelete, mapIds));
        futures.put(future, future);

        //every so often, check any previously submitted tasks to see if they're complete or in error
        if (futures.size() % 1000 == 0) {
            checkFutures(false);
        }
    }

    private void checkFutures(boolean forceLock) throws Exception {

        try {

            //when finishing processing, we want to guarantee a lock, but when doing an interim check,
            //we just try to get the lock and backoff if we can't
            if (forceLock) {
                futuresLock.lock();
            } else {
                if (!futuresLock.tryLock()) {
                    return;
                }
            }

            //check all the futures to see if any raised an error. Also, remove any futures that
            //we know have completed without error.
            List<Future> futuresCompleted = new ArrayList<>();
            Iterator<Future> it = futures.keySet().iterator();
            while (it.hasNext()) {
                Future future = it.next();
                if (future.isDone()) {
                    try {
                        //just calling get on the future will cause any exception to be raised in this thread
                        future.get();
                        futuresCompleted.add(future);
                    } catch (Exception ex) {
                        throw (Exception)ex.getCause();
                    }
                }
            }

            //remove any we know have completed
            for (Future future: futuresCompleted) {
                futures.remove(future);
            }

        } finally {
            futuresLock.unlock();
        }
    }


    private UUID getAdminBatchId() {
        if (adminBatchId == null) {

            try {
                batchIdLock.lock();

                //make sure to check if it's still null, as another thread may have created the ID while we were waiting to batchIdLock
                if (adminBatchId == null) {
                    adminBatchId = UUIDs.timeBased();
                    saveExchangeBatch(adminBatchId);

                    countResourcesDeleted.put(adminBatchId, new AtomicInteger());
                    countResourcesSaved.put(adminBatchId, new AtomicInteger());
                }
            } finally {
                batchIdLock.unlock();
            }
        }
        return adminBatchId;
    }
    private UUID getPatientBatchId(String patientId) {
        UUID patientBatchId = patientBatchIdMap.get(patientId);
        if (patientBatchId == null) {

            try {
                batchIdLock.lock();

                //make sure to check if it's still null, as another thread may have created the ID while we were waiting to batchIdLock
                patientBatchId = patientBatchIdMap.get(patientId);
                if (patientBatchId == null) {
                    patientBatchId = UUIDs.timeBased();
                    patientBatchIdMap.put(patientId, patientBatchId);
                    saveExchangeBatch(patientBatchId);

                    countResourcesDeleted.put(patientBatchId, new AtomicInteger());
                    countResourcesSaved.put(patientBatchId, new AtomicInteger());
                }
            } finally {
                batchIdLock.unlock();
            }
        }
        return patientBatchId;
    }
    private void saveExchangeBatch(UUID batchId) {
        ExchangeBatch exchangeBatch = new ExchangeBatch();
        exchangeBatch.setBatchId(batchId);
        exchangeBatch.setExchangeId(exchangeId);
        exchangeBatch.setInsertedAt(new Date());
        exchangeBatchRepository.save(exchangeBatch);
    }

    /**
     * called after all content has been processed. It blocks until all operations have
     * been completed in the thread pool, then returns the distinct batch IDs created
     */
    public List<UUID> getBatchIdsCreated() throws Exception {

        //shutdown the threadpool and wait for all runnables to complete
        threadPool.shutdown();
        while (!threadPool.awaitTermination(1, TimeUnit.MINUTES)) {
            LOG.trace("Waiting for thread pool to complete {} tasks", threadPoolQueueSize.get());
        }

        //check any remaining futures, to see if any exceptions were raised
        checkFutures(true);

        logResults();

        return getAllBatchIds();
    }

    private void logResults() {

        int totalSaved = 0;
        int totalDeleted = 0;

        LOG.info("CSV processing completed");

        int saved = countResourcesSaved.get(adminBatchId).get();
        int deleted = countResourcesDeleted.get(adminBatchId).get();
        LOG.info("Saved {} and deleted {} non-patient resources", saved, deleted);
        totalSaved += saved;
        totalDeleted += deleted;

        Iterator<String> it = patientBatchIdMap.keySet().iterator();
        while (it.hasNext()) {
            String patientId = it.next();
            UUID batchId = patientBatchIdMap.get(patientId);

            saved = countResourcesSaved.get(batchId).get();
            deleted = countResourcesDeleted.get(batchId).get();
            LOG.info("Saved {} and deleted {} resources for patient {}", saved, deleted, patientId);
            totalSaved += saved;
            totalDeleted += deleted;
        }

        LOG.info("CSV processing completed, saving {} resources, deleting {} for {} distinct patients", totalSaved, totalDeleted, patientBatchIdMap.size());
    }

    private List<UUID> getAllBatchIds() {
        List<UUID> batchIds = new ArrayList<>();
        if (adminBatchId != null) {
            batchIds.add(adminBatchId);
        }
        Iterator<UUID> it = patientBatchIdMap.values().iterator();
        while (it.hasNext()) {
            UUID batchId = it.next();
            batchIds.add(batchId);
        }
        return batchIds;
    }

    private static boolean isPatientResource(Resource resource) {
        Class cls = resource.getClass();

        if (patientResourceClasses == null) {
            Set<Class> set = new HashSet<>();
            set.add(AllergyIntolerance.class);
            set.add(Appointment.class);
            set.add(Condition.class);
            set.add(DiagnosticOrder.class);
            set.add(DiagnosticReport.class);
            set.add(Encounter.class);
            set.add(EpisodeOfCare.class);
            set.add(FamilyMemberHistory.class);
            set.add(Immunization.class);
            set.add(MedicationOrder.class);
            set.add(MedicationStatement.class);
            set.add(Observation.class);
            set.add(Order.class);
            set.add(Patient.class);
            set.add(Procedure.class);
            set.add(ProcedureRequest.class);
            set.add(ReferralRequest.class);
            set.add(RelatedPerson.class);
            set.add(Specimen.class);

            //although Slot isn't technically linked to a patient, it is saved at the same time as
            //Appointment resources, so should be treated as one
            set.add(Slot.class);

            patientResourceClasses = set;
        }

        return patientResourceClasses.contains(cls);
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getSystemId() {
        return systemId;
    }

    class MapAndSaveResourceTask implements Callable {

        private Resource resource = null;
        private UUID batchUuid = null;
        private boolean isDelete = false;
        private boolean mapIds = false;

        public MapAndSaveResourceTask(Resource resource, UUID batchUuid, boolean isDelete, boolean mapIds) {
            this.resource = resource;
            this.batchUuid = batchUuid;
            this.isDelete = isDelete;
            this.mapIds = mapIds;
        }

        @Override
        public Object call() throws Exception {

            try {
                if (mapIds) {
                    IdHelper.mapIds(serviceId, systemId, resource);
                }

                List<Resource> list = new ArrayList<>();
                list.add(resource);

                if (isDelete) {
                    storageService.exchangeBatchDelete(exchangeId, batchUuid, list);
                } else {
                    storageService.exchangeBatchUpdate(exchangeId, batchUuid, list);
                }

                threadPoolQueueSize.decrementAndGet();

            } catch (Exception ex) {
                throw new TransformException("Exception mapping or storing " + resource.getResourceType() + " " + resource.getId(), ex);
            }

            return null;
        }
    }
}

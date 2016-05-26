package org.endeavourhealth.core.data.ehr.models;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;
import java.util.UUID;

@Table(keyspace = "ehr", name = "person_resource")
public class PersonResource {
    @PartitionKey
    @Column(name = "person_id")
    private UUID personId;
    @ClusteringColumn(0)
    @Column(name = "resource_type")
    private String resourceType;
    @ClusteringColumn(1)
    @Column(name = "service_id")
    private UUID serviceId;
    @ClusteringColumn(2)
    @Column(name = "system_instance_id")
    private UUID systemInstanceId;
    @ClusteringColumn(3)
    @Column(name = "resource_id")
    private String resourceId;
    @Column(name = "effective_date")
    private Date effectiveDate;
    private String version;
    @Column(name = "last_updated")
    private Date lastUpdated;
    @Column(name = "resource_metadata")
    private String resourceMetadata;
    @Column(name = "schema_version")
    private String schemaVersion;
    @Column(name = "resource_data")
    private String resourceData;

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getSystemInstanceId() {
        return systemInstanceId;
    }

    public void setSystemInstanceId(UUID systemInstanceId) {
        this.systemInstanceId = systemInstanceId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getResourceMetadata() {
        return resourceMetadata;
    }

    public void setResourceMetadata(String resourceMetadata) {
        this.resourceMetadata = resourceMetadata;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getResourceData() {
        return resourceData;
    }

    public void setResourceData(String resourceData) {
        this.resourceData = resourceData;
    }
}

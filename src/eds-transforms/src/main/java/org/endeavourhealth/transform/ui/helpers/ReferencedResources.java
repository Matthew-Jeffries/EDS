package org.endeavourhealth.transform.ui.helpers;

import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.core.utility.StreamExtension;
import org.endeavourhealth.transform.fhir.ReferenceHelper;
import org.endeavourhealth.transform.ui.models.resources.admin.UILocation;
import org.endeavourhealth.transform.ui.models.resources.admin.UIOrganisation;
import org.endeavourhealth.transform.ui.models.resources.admin.UIPractitioner;
import org.endeavourhealth.transform.ui.transforms.admin.UILocationTransform;
import org.endeavourhealth.transform.ui.transforms.admin.UIOrganisationTransform;
import org.endeavourhealth.transform.ui.transforms.admin.UIPractitionerTransform;
import org.hl7.fhir.instance.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class ReferencedResources {
    private List<Practitioner> practitioners;
    private List<UIPractitioner> uiPractitioners;
    private List<Organization> organisations;
    private List<UIOrganisation> uiOrganisations;
    private List<Location> locations;
    private List<UILocation> uiLocations;

    public void setPractitioners(List<Practitioner> practitioners) {
        this.practitioners = practitioners;

        this.uiPractitioners = practitioners
                .stream()
                .map(t -> UIPractitionerTransform.transform(t))
                .collect(Collectors.toList());
    }

    public UIPractitioner getUIPractitioner(Reference reference) {
        String referenceId = ReferenceHelper.getReferenceId(reference, ResourceType.Practitioner);

        if (StringUtils.isEmpty(referenceId))
            return null;

        return this
                .uiPractitioners
                .stream()
                .filter(t -> t.getId().equals(referenceId))
                .collect(StreamExtension.firstOrNullCollector());
    }

    public UIOrganisation getUIOrganisation(Reference reference) {
        String referenceId = ReferenceHelper.getReferenceId(reference, ResourceType.Organization);

        if (StringUtils.isEmpty(referenceId))
            return null;

        return this
                .uiOrganisations
                .stream()
                .filter(t -> t.getId().equals(referenceId))
                .collect(StreamExtension.firstOrNullCollector());
    }

    public void setOrganisations(List<Organization> organisations) {
        this.organisations = organisations;

        this.uiOrganisations = organisations
                .stream()
                .map(t -> UIOrganisationTransform.transform(t))
                .collect(Collectors.toList());
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;

        this.uiLocations = locations
                .stream()
                .map(t -> UILocationTransform.transform(t))
                .collect(Collectors.toList());
    }
}
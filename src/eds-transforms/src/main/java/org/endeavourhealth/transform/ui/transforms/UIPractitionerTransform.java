package org.endeavourhealth.transform.ui.transforms;

import org.endeavourhealth.transform.ui.helpers.NameHelper;
import org.endeavourhealth.transform.ui.models.UIPractitioner;
import org.hl7.fhir.instance.model.Practitioner;

public class UIPractitionerTransform {
    public static UIPractitioner transform(Practitioner practitioner) {

        return new UIPractitioner()
                .setName(NameHelper.transform(practitioner.getName()))
                .setActive(practitioner.getActive());
    }
}

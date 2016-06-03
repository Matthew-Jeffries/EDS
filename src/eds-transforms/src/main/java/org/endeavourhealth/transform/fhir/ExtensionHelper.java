package org.endeavourhealth.transform.fhir;

import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Type;

public class ExtensionHelper {

    public static Extension createExtension(String uri, Type value) {
        return new Extension()
                .setUrl(uri)
                .setValue(value);
    }
}

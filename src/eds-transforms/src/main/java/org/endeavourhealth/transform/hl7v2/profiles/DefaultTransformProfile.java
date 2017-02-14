package org.endeavourhealth.transform.hl7v2.profiles;

import org.endeavourhealth.transform.hl7v2.parser.ParseException;
import org.endeavourhealth.transform.hl7v2.parser.Segment;
import org.endeavourhealth.transform.hl7v2.parser.messages.AdtMessage;

import java.util.HashMap;

public class DefaultTransformProfile implements TransformProfile {
    public HashMap<String, Class<? extends Segment>> getZSegments() {
        return new HashMap<>();
    }

    public AdtMessage preTransform(AdtMessage sourceMessage) throws ParseException {
        return sourceMessage;
    }
}

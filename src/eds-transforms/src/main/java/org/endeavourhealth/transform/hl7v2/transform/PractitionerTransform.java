package org.endeavourhealth.transform.hl7v2.transform;

import org.endeavourhealth.transform.hl7v2.parser.ParseException;
import org.endeavourhealth.transform.hl7v2.parser.datatypes.Xcn;
import org.endeavourhealth.transform.hl7v2.parser.messages.AdtMessage;
import org.endeavourhealth.transform.hl7v2.parser.segments.EvnSegment;
import org.endeavourhealth.transform.hl7v2.parser.segments.MshSegment;
import org.endeavourhealth.transform.hl7v2.parser.segments.Pd1Segment;
import org.endeavourhealth.transform.hl7v2.transform.converters.IdentifierConverter;
import org.endeavourhealth.transform.hl7v2.transform.converters.NameConverter;
import org.hl7.fhir.instance.model.HumanName;
import org.hl7.fhir.instance.model.Identifier;
import org.hl7.fhir.instance.model.Practitioner;

import java.util.ArrayList;
import java.util.List;

public class PractitionerTransform {
    public static List<Practitioner> fromHl7v2(AdtMessage source) throws ParseException, TransformException {
        List<Practitioner> practitioners = new ArrayList<>();

        MshSegment mshSegment = source.getMshSegment();
        EvnSegment evnSegment = source.getEvnSegment();
        Pd1Segment pd1Segment = source.getPd1Segment();

        for (Xcn xcn : evnSegment.getOperators())
            practitioners.add(transform(xcn, mshSegment.getSendingFacility()));

        for (Xcn xcn : pd1Segment.getPatientPrimaryCareProvider())
            practitioners.add(transform(xcn, mshSegment.getSendingFacility()));

        return practitioners;
    }

    private static Practitioner transform(Xcn xcn, String sendingFacility) throws TransformException {
        if (xcn == null)
            return null;

        Practitioner practitioner = new Practitioner();

        practitioner.setName(NameConverter.convert(xcn));

        Identifier identifier = IdentifierConverter.convert(xcn, sendingFacility);

        if (identifier != null)
            practitioner.addIdentifier(identifier);

        return practitioner;
    }
}

package org.endeavourhealth.ui.querydocument;

import org.endeavourhealth.ui.XmlSerializer;
import org.endeavourhealth.ui.database.definition.DbItem;
import org.endeavourhealth.ui.querydocument.models.LibraryItem;
import org.endeavourhealth.ui.querydocument.models.ObjectFactory;
import org.endeavourhealth.ui.querydocument.models.QueryDocument;
import org.endeavourhealth.ui.querydocument.models.Report;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public abstract class QueryDocumentSerializer {

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static final String XSD = "QueryDocument.xsd";

    public static LibraryItem readLibraryItemFromItem(DbItem item) throws ParserConfigurationException, JAXBException, IOException, SAXException {
        return XmlSerializer.deserializeFromString(LibraryItem.class, item.getXmlContent(), XSD);
    }
    public static Report readReportFromItem(DbItem item) throws ParserConfigurationException, JAXBException, IOException, SAXException {
        return XmlSerializer.deserializeFromString(Report.class, item.getXmlContent(), XSD);
    }
    public static QueryDocument readQueryDocumentFromItem(DbItem item) throws ParserConfigurationException, JAXBException, IOException, SAXException {
        return XmlSerializer.deserializeFromString(QueryDocument.class, item.getXmlContent(), XSD);
    }


    public static LibraryItem readLibraryItemFromXml(String xml) throws ParserConfigurationException, JAXBException, IOException, SAXException {
        return XmlSerializer.deserializeFromString(LibraryItem.class, xml, XSD);
    }
    public static Report readReportFromXml(String xml) throws ParserConfigurationException, JAXBException, IOException, SAXException {
        return XmlSerializer.deserializeFromString(Report.class, xml, XSD);
    }
    public static QueryDocument readQueryDocumentFromXml(String xml) throws ParserConfigurationException, JAXBException, IOException, SAXException {
        return XmlSerializer.deserializeFromString(QueryDocument.class, xml, XSD);
    }


    public static String writeToXml(QueryDocument q) {
        if (q.getFolder().isEmpty()
                && q.getLibraryItem().isEmpty()
                && q.getReport().size() == 1) {

            Report report = q.getReport().get(0);
            JAXBElement element = OBJECT_FACTORY.createReport(report);
            return XmlSerializer.serializeToString(element, XSD);

        } else if (q.getFolder().isEmpty()
                && q.getLibraryItem().size() == 1
                && q.getReport().isEmpty()) {

            LibraryItem libraryItem = q.getLibraryItem().get(0);
            JAXBElement element = OBJECT_FACTORY.createLibraryItem(libraryItem);
            return XmlSerializer.serializeToString(element, XSD);
        }
        else
        {
            JAXBElement element = OBJECT_FACTORY.createQueryDocument(q);
            return XmlSerializer.serializeToString(element, XSD);
        }
    }



}

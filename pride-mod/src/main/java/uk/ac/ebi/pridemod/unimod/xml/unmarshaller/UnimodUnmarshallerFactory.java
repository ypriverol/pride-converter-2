package uk.ac.ebi.pridemod.unimod.xml.unmarshaller;

import org.apache.log4j.Logger;
import uk.ac.ebi.pridemod.unimod.model.ModelConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * @author Florian Reisinger
 * @author rwang
 * @since 0.1
 */
public class UnimodUnmarshallerFactory {

    private static final Logger logger = Logger.getLogger(UnimodUnmarshallerFactory.class);

    private static UnimodUnmarshallerFactory instance = new UnimodUnmarshallerFactory();

    private static JAXBContext jc = null;

    private UnimodUnmarshallerFactory() {
    }

    public static UnimodUnmarshallerFactory getInstance() {
        return instance;
    }

    public Unmarshaller initializeUnmarshaller() {

        try {
            // Lazy caching of the JAXB Context.
            if (jc == null) {
                jc = JAXBContext.newInstance(ModelConstants.MODEL_PKG);
            }

            //create unmarshaller
            Unmarshaller pum = jc.createUnmarshaller();
            logger.info("Unmarshaller Initialized");

            return pum;

        } catch (JAXBException e) {
            logger.error("UnmarshallerFactory.initializeUnmarshaller", e);
            throw new IllegalStateException("Could not initialize unmarshaller", e);
        }
    }
}

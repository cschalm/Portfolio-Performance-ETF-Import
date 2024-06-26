import models.Security;
import org.junit.Test;
import org.schalm.test.AbstractTest;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import xml.XmlHelper;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MainTest extends AbstractTest {
    private static final Logger logger = Logger.getLogger(Main.class.getCanonicalName());
    private Main main = new Main();
    XmlHelper xmlHelper = new XmlHelper();

    @Test
    public void addClassificationData() throws IOException, ParserConfigurationException, SAXException {
        Document document = xmlHelper.readXmlStream("src/test/resources/EtfSecurity.xml");
        NodeList securityNodes = document.getElementsByTagName("security");
        assertNotNull(securityNodes);
        assertEquals(1, securityNodes.getLength());

        List<Security> securities = main.addClassificationData(securityNodes);
        assertEquals(1, securities.size());
    }

    @Test
    public void getEnvironment() {
        Map<String, String> env = System.getenv();
        logger.info(env.toString());
    }

    @Test
    public void getSystemProperties() {
        Properties properties = System.getProperties();
        logger.info(properties.toString());
    }

}
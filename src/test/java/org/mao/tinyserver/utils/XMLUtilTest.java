package org.mao.tinyserver.utils;

import org.junit.Before;
import org.junit.Test;
import org.mao.tinyserver.config.ServerConfigBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class XMLUtilTest {

    private static final Logger logger = LoggerUtil.getLogger(XMLUtil.class);
    Document document = null;

    @Before
    public void before(){
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.parse(new File(ServerConfigBuilder.DEFAULT_CONFIG_FILE));
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void findNodeByURL() {
        String name = null;
        try {
            name = XMLUtil.findContentByURL("/serverconfig/port", document);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        assertEquals("8083", name);

    }
}
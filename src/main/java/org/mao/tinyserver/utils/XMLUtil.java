package org.mao.tinyserver.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.logging.Logger;

public class XMLUtil {

    public static final XPath xpath = XPathFactory.newInstance().newXPath();
    private static final Logger logger = LoggerUtil.getLogger(XMLUtil.class);

    public static String findContentByURL(String url, Document document) throws XPathExpressionException, NullPointerException {

        Node rootNode = (Node)xpath.evaluate(url, document, XPathConstants.NODE);
        String content = null;
        content = rootNode.getChildNodes().item(0).getNodeValue();

        return content;
    }

}

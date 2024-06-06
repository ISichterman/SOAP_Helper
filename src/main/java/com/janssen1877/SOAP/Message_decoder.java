package com.janssen1877.SOAP;

import nl.copernicus.niklas.transformer.*;
import nl.copernicus.niklas.transformer.context.ComponentContext;
import nl.copernicus.niklas.transformer.context.NiklasLogger;
import nl.copernicus.niklas.transformer.context.RoutingContext;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Message_decoder implements NiklasComponent<String, String>, NiklasLoggerAware, RoutingContextAware, ComponentContextAware {

  protected NiklasLogger log;
  protected RoutingContext rc;
  protected ComponentContext cc;

  @Override
  public String process(Header header, String payload) throws NiklasComponentException {
    String xPathExpression = (String) cc.getProperties().get("xpath");
    if(xPathExpression == null){
      throw new NiklasComponentException("Manditory property \"xpath\" not set up!");
    }
    Boolean errorOnEmpty = (Boolean) cc.getProperties().get("errorOnEmpty");
    if(errorOnEmpty == null){
      errorOnEmpty = false;
    }
    String ExpectedAction = (String) cc.getProperties().get("ExpectedAction");
    if(ExpectedAction != null){
      List<String> allowedActions = Arrays.asList(ExpectedAction.split(","));

      Object headerPropsObject = header.getProperties().getOrDefault("http.headers",
              header.getProperty("http_headers"));
      HashMap<String, String> headerProps = (HashMap<String, String>) headerPropsObject;
      if (headerProps == null) {
        throw new NiklasComponentException("unable to determine the http header props using http.headers");
      }
      String SoapAction = headerProps.get("soapaction");
      if(SoapAction == null){
        throw new NiklasComponentException("Unable to handle request without a valid action parameter. Please supply a valid soap action.");
      }
      if(! allowedActions.contains(SoapAction)){
         throw new NiklasComponentException("Server did not recognize the value of HTTP Header SOAPAction: " + SoapAction);
      }
    }
    String outputType = (String) cc.getProperties().get("outputType");


    //create dom document from string
    Document document = createXMLDocument(payload);
    //create xpath object
    XPath xPath = createXPath();

    String result;
    try {
      if(outputType != null && outputType.equals("text")){
        result = (String) xPath.evaluate(xPathExpression, document, XPathConstants.STRING);
      }else {
        NodeList nodes = (NodeList) xPath.evaluate(xPathExpression, document, XPathConstants.NODESET);
        Document newXmlDocument = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        for (int i = 0; i < nodes.getLength(); i++) {
          Node node = nodes.item(i);
          Node copyNode = newXmlDocument.importNode(node, true);
          newXmlDocument.appendChild(copyNode);
        }

        result = docToString(newXmlDocument);
      }

      if(result.isEmpty()){
        if(errorOnEmpty){
          throw new NiklasComponentException("No data to process!");
        }else {
          result = payload;
        }
      }
    } catch (XPathExpressionException | ParserConfigurationException e) {
      throw new NiklasComponentException("xPath failure", e);
    }

    return result;
  }

  private Document createXMLDocument(String xml) throws NiklasComponentException {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      return documentBuilderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

    } catch (ParserConfigurationException | SAXException | IOException x) {
      throw new NiklasComponentException("unable to convert string to xml document", x);
    }
  }

  private XPath createXPath() {
    XPath xPath = XPathFactory.newInstance().newXPath();
    return xPath;
  }

  private String docToString(Document doc) {
    try {
      StringWriter sw = new StringWriter();
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

      transformer.transform(new DOMSource(doc), new StreamResult(sw));
      return sw.toString();
    } catch (Exception ex) {
      throw new RuntimeException("Error converting to String", ex);
    }
  }

  @Override
  public void setLogger(NiklasLogger nl) {
    this.log = nl;
  }

  @Override
  public void setRoutingContext(RoutingContext routingContext) {
    this.rc = routingContext;
  }

  @Override
  public void setComponentContext(ComponentContext cc) {
    this.cc = cc;
  }
}

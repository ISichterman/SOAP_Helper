import com.janssen1877.SOAP.Message_decoder;
import com.janssen1877.SOAP.Message_encoder;
import nl.copernicus.niklas.test.FunctionalTestCase;
import nl.copernicus.niklas.test.MockupComponentContext;
import nl.copernicus.niklas.test.MockupHeader;
import nl.copernicus.niklas.transformer.Header;
import nl.copernicus.niklas.transformer.NiklasComponentException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;


public class soaphelperTest extends FunctionalTestCase {

    String template = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><PostXml xmlns=\"urn:microsoft-dynamics-schemas/codeunit/DIPost\"><xml>{toReplace}</xml></PostXml></soap:Body></soap:Envelope>";
    String xpath = "//PostXml_Result/return_value";
    String xpath2 = "//*[local-name()='Body']/*[local-name()='submit']/*[local-name()='Root']/*";

    public void testencoder(String fileName, String template, String action, Boolean escape) throws Exception {
        File TEST_FILE = new File(fileName);
        // initialise the transformer

        this.setComponentContext(new MockupComponentContext());
        if(template != null){
            this.getComponentContext().getProperties().put("template", template);
        }
        if(action != null){
            this.getComponentContext().getProperties().put("action", action);
        }
        if(escape != null){
            this.getComponentContext().getProperties().put("escape", escape);
        }

        Message_encoder transformerInstance = getTransformerInstance(Message_encoder.class);

        Header hdr = new MockupHeader();
        String result = transformerInstance.process(hdr, FileUtils.readFileToString(TEST_FILE));
        System.out.println(result);
        if (action != null) {
            assert hdr.getProperty("SoapAction").equals(action);
        }
        super.destroy(transformerInstance);
    }

    public void testdecoder(String fileName, String xPath, String AllowedActions, Boolean ErroronEmpty, String outputType) throws Exception {
        File TEST_FILE = new File(fileName);
        // initialise the transformer
        this.setComponentContext(new MockupComponentContext());
        if(xPath != null){
            this.getComponentContext().getProperties().put("xpath", xPath);
        }
        if(AllowedActions != null) {
            this.getComponentContext().getProperties().put("ExpectedAction", AllowedActions);
        }
        if(ErroronEmpty != null){
            this.getComponentContext().getProperties().put("errorOnEmpty", ErroronEmpty);
        }
        if(outputType != null){
            this.getComponentContext().getProperties().put("outputType", outputType);
        }

        Message_decoder transformerInstance = getTransformerInstance(Message_decoder.class);

        Header hdr = new MockupHeader();
        HashMap<String,String> Headers = new HashMap<String,String>();
        Headers.put("Keep-Alive", "true");
        Headers.put("content-length", "7119");
        Headers.put("expect", "100-continue");
        Headers.put("x-forwarded-host", "jds.coperniapps.nl");
        Headers.put("soapaction", "urn:allin:webservice:standard:submit");
        hdr.setProperty("http.headers", Headers);
        String result = transformerInstance.process(hdr, FileUtils.readFileToString(TEST_FILE));
        System.out.println(result);
        super.destroy(transformerInstance);
    }


    @Test(expected= NiklasComponentException.class)
    public void testEncodeNoTemplate() throws Exception {
        testencoder("src/test/resources/testinput.xml",null, "test", true);
    }

    @Test
    public void testEncodeNoAction() throws Exception {
        testencoder("src/test/resources/testinput.xml",template, null, true);
    }

    @Test(expected= NiklasComponentException.class)
    public void testEncodeNoescape() throws Exception {
        testencoder("src/test/resources/testinput.xml",template, "test", null);
    }

    @Test
    public void testEncodeEscape() throws Exception {
        testencoder("src/test/resources/testinput.xml",template, "test", true);
    }

    @Test
    public void testEncodeNotEscape() throws Exception {
        testencoder("src/test/resources/testinput.xml",template, "test", false);
    }

    @Test(expected= NiklasComponentException.class)
    public void testDecodeNoxpath() throws Exception {
        testdecoder("src/test/resources/testinput.xml",null, null, null, "text");
    }

    @Test
    public void testDecode() throws Exception {
        testdecoder("src/test/resources/testoutput.xml",xpath, null, null, "text");
    }

    @Test
    public void testDecodeActionGood() throws Exception {
        testdecoder("src/test/resources/testoutput.xml",xpath, "urn:allin:webservice:standard:submit", false, "text");
    }

    @Test(expected= NiklasComponentException.class)
    public void testDecodeActionError() throws Exception {
        testdecoder("src/test/resources/testoutput.xml",xpath, "BadAction", false, "text");
    }

    @Test(expected= NiklasComponentException.class)
    public void testEmptyxPath() throws Exception {
        testdecoder("src/test/resources/testinput.xml",xpath, "urn:allin:webservice:standard:submit", true, "text");
    }

    @Test
    public void testEmptyxPathnoError() throws Exception {
        testdecoder("src/test/resources/testinput.xml",xpath, "urn:allin:webservice:standard:submit", false, "text");
    }

    @Test
    public void testsoap() throws Exception {
        testdecoder("src/test/resources/testsoapinput.xml",xpath2, "urn:allin:webservice:standard:submit", false, "xml");
    }

}

package edu.memphis.iis.tdc.annotator.data;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.UserErrorException;

public class HttpClientTest {
    HttpClient client = null;
    LocalTestServer server = null;
    
    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        
        server = new LocalTestServer(null, null);

        server.register("/hitme/*", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest req, HttpResponse resp, HttpContext ctx)
                    throws HttpException, IOException 
            {
                resp.setStatusCode(200);
                resp.setEntity(new StringEntity("Hello World"));
            }
        });
        
        server.start();
        
        client = new HttpClient();
    }
    
    private String buildURL(String path) {
        //YES - on forward slash after http:
        return "http:/" + server.getServiceAddress().toString() + path;
    }
    
    @After
    public void tearDown() throws Exception {
        server.stop();
        LogManager.resetConfiguration();
    }

    @Test
    public void testGetExisting() throws Exception {
        String resp = client.execute(new HttpGet(buildURL("/hitme/")));
        assertTrue(resp.contains("Hello World"));
    }
    
    @Test(expected=UserErrorException.class)
    public void testBadReturnCode() throws Exception {
        client.execute(new HttpGet(buildURL("/not-gonna-be-there/")));
    }
    
    @Test(expected=UserErrorException.class)
    public void testBadRequest() throws Exception {
        (new HttpClient()).execute(new HttpGet("https://localhost:1/nope.html"));
    }
}

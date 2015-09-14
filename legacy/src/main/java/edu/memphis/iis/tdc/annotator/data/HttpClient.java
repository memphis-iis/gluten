package edu.memphis.iis.tdc.annotator.data;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import edu.memphis.iis.tdc.annotator.UserErrorException;

/**
 * Our VERY simple wrapper around the http client from Apache.
 * Currently used for oauth2 stuff and nothing else
 */
public class HttpClient {
    private static final Logger logger = Logger.getLogger(HttpClient.class);
    
    //Our generic HTTP client execution - we expect a 200 and a body to come back 
    public String execute(HttpRequestBase post) throws UserErrorException {
        CloseableHttpClient client = null;
        CloseableHttpResponse resp = null;
        try {
            client = HttpClients.createDefault();
            resp = client.execute(post);
            HttpEntity entity = resp.getEntity();
            String body = EntityUtils.toString(entity);
            
            int statCode = resp.getStatusLine().getStatusCode(); 
            if (statCode != 200) {
                String msg = "Expected 200 but got " + statCode;
                logger.warn("About to throw " + msg + " body was " + body);
                throw new RuntimeException(msg);
            }
            
            return body;
        } 
        catch (Throwable e) {
            throw new UserErrorException("Error performing remote server post", e);
        }
        finally {
            HttpClientUtils.closeQuietly(resp);
            HttpClientUtils.closeQuietly(client);
        }
    }
}

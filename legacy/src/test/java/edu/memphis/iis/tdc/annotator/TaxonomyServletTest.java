package edu.memphis.iis.tdc.annotator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.model.DialogAct;
import edu.memphis.iis.tdc.annotator.model.Taxonomy;

public class TaxonomyServletTest {
    TaxonomyServlet servlet;
    
    ConfigContext ctx;
    HttpSession session;
    HttpServletRequest request;
    HttpServletResponse response;
    
    StringWriter outputBuf;
    
    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        
        servlet = new TaxonomyServlet();
        
        ctx = mock(ConfigContext.class);
        ConfigContext.overwriteCurrentInstance(ctx);
        
        session = mock(HttpSession.class);
        
        request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.localhost:1234/annotator/tax"));
        when(request.getQueryString()).thenReturn("kermit=frog&gonzo=weirdo");
        
        outputBuf = new StringWriter();
        PrintWriter output = new PrintWriter(outputBuf);
        
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(output);
    }
    
    @After
    public void tearDown() {
        validateMockitoUsage();
        ConfigContext.overwriteCurrentInstance(null);
        LogManager.resetConfiguration();
    }

    @Test
    public void testGet() throws Exception {
        Serializer serializer = new Persister();
        Taxonomy taxIn = serializer.read(Taxonomy.class, stringStream(
                "<?xml version='1.0' encoding='utf-8'?>\r\n" +
                "<taxonomy><dialogModes /><dialogActs /></taxonomy>\r\n"));
        
        taxIn.getDialogModes().add("Mode1");
        taxIn.getDialogModes().add("Mode2");
        taxIn.getDialogActs().add(mockDialogAct("act1"));
        taxIn.getDialogActs().add(mockDialogAct("act2"));
        
        when(ctx.getTaxonomy()).thenReturn(taxIn);
        
        when(session.getAttribute("user.email")).thenReturn("tester@test.memphis.edu");
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        servlet.doGet(request, response);
        
        verify(response).setContentType("application/javascript");
        
        String rawJson = outputBuf.toString()
            .replace("var taxonomy = ", "")
            .replace(";", "")
            .trim();
        JSONObject taxOut = new JSONObject(rawJson);
        assertEquals(2, taxOut.getJSONArray("dialogModes").length());
        assertEquals(2, taxOut.getJSONObject("dialogActs").getJSONObject("act1").getJSONArray("subtypes").length());
        assertEquals(2, taxOut.getJSONObject("dialogActs").getJSONObject("act2").getJSONArray("subtypes").length());
        assertEquals(2, taxOut.getJSONArray("dialogActNames").length());
    }
    
    private static InputStream stringStream(String s) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(s.trim().getBytes(StandardCharsets.UTF_8));
    }
    
    private static DialogAct mockDialogAct(String name) {
        DialogAct act = mock(DialogAct.class);
        when(act.getName()).thenReturn(name);
        
        List<String> sts = new ArrayList<String>();
        sts.add(name + ":st1");
        sts.add(name + ":st2");
        when(act.getSubtypes()).thenReturn(sts);
        return act;
    }
    
    @Test
    public void testGetNeedsLogin() throws Exception {
        when(ctx.getString("annotator.test.user.email")).thenReturn("");
        when(ctx.getString("annotator.test.user.name")).thenReturn("");
        when(ctx.getString("annotator.test.user.notest")).thenReturn("true");
        
        when(ctx.getString("oauth2.google.auth.uri")).thenReturn("https://accounts.google.com/o/oauth2/auth");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        
        when(session.getAttribute("user.email")).thenReturn("");
        when(session.getAttribute("user.fullname")).thenReturn("");
        
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.localhost:1234/annotator/tax"));
        when(request.getQueryString()).thenReturn("kermit=frog&gonzo=weirdo");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("test.localhost");
        when(request.getServerPort()).thenReturn(1234);
        when(request.getContextPath()).thenReturn("annotator");
        
        servlet.doGet(request, response);
        
        assertTrue(StringUtils.isBlank(outputBuf.toString()));
        
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader(matches("Location"), startsWith("https://accounts.google.com/o/oauth2/auth"));
    }
    
    @Test(expected=RuntimeException.class)
    public void testPost() throws Exception {
        TaxonomyServlet postWrapper = new TaxonomyServlet() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                try {
                    doProtectedPost(request, response);
                }
                catch(UserErrorException e) {
                    throw new RuntimeException("Gotcha", e);
                }
            }
        };
        
        postWrapper.doPost(null, null);
    }
}

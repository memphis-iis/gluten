package edu.memphis.iis.tdc.annotator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.DialogAct;
import edu.memphis.iis.tdc.annotator.model.Taxonomy;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;

public class MainServletTest {
    MainServlet servlet;
    
    ConfigContext ctx;
    HttpSession session;
    HttpServletRequest request;
    HttpServletResponse response;
    
    StringWriter outputBuf;
    
    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        
        servlet = new MainServlet();
        
        ctx = mock(ConfigContext.class);
        ConfigContext.overwriteCurrentInstance(ctx);
        
        session = mock(HttpSession.class);
        
        request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.localhost:1234/annotator/home"));
        
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
    public void testInitFires() throws Exception {
        Taxonomy tax = mock(Taxonomy.class);
        
        List<DialogAct> acts = new ArrayList<DialogAct>();
        acts.add(mock(DialogAct.class));
        acts.add(mock(DialogAct.class));
        
        when(ctx.getTaxonomy()).thenReturn(tax);
        when(tax.getDialogActs()).thenReturn(acts);
        
        servlet.init();
        
        verify(tax).getDialogActs();
    }
    
    @Test
    public void testVanillaGet() throws Exception {
        final String USR_EMAIL = "tester@test.memphis.edu";
        
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        TranscriptService tserv = mock(TranscriptService.class);
        
        List<TranscriptSession> pendList = new ArrayList<TranscriptSession>();
        List<TranscriptSession> progList = new ArrayList<TranscriptSession>();
        List<TranscriptSession> compList = new ArrayList<TranscriptSession>();
        
        when(tserv.getUserTranscripts(State.Pending, USR_EMAIL)).thenReturn(pendList);
        when(tserv.getUserTranscripts(State.InProgress, USR_EMAIL)).thenReturn(progList);
        when(tserv.getUserTranscripts(State.Completed, USR_EMAIL)).thenReturn(compList);
        
        when(ctx.getTranscriptService()).thenReturn(tserv);
        
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/view/home.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);
        
        verify(request).setAttribute(Const.REQ_SESS_PEND, pendList);
        verify(request).setAttribute(Const.REQ_SESS_INPROG, progList);
        verify(request).setAttribute(Const.REQ_SESS_COMP, compList);
        verify(request).setAttribute(Const.REQ_IS_ASSIGNER, false);
        verify(request).setAttribute(Const.REQ_IS_VERIFIER, false);
        verify(dispatcher).forward(request, response);
    }
    
    @Test
    public void testGetLogout() throws Exception {
        final String USR_EMAIL = "tester@test.memphis.edu";
        
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        TranscriptService tserv = mock(TranscriptService.class);
        
        List<TranscriptSession> pendList = new ArrayList<TranscriptSession>();
        List<TranscriptSession> progList = new ArrayList<TranscriptSession>();
        List<TranscriptSession> compList = new ArrayList<TranscriptSession>();
        
        when(tserv.getUserTranscripts(State.Pending, USR_EMAIL)).thenReturn(pendList);
        when(tserv.getUserTranscripts(State.InProgress, USR_EMAIL)).thenReturn(progList);
        when(tserv.getUserTranscripts(State.Completed, USR_EMAIL)).thenReturn(compList);
        
        when(ctx.getTranscriptService()).thenReturn(tserv);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("someserver");
        when(request.getServerPort()).thenReturn(1234);
        when(request.getContextPath()).thenReturn("annotator");
        
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/view/home.jsp")).thenReturn(dispatcher);

        when(request.getParameter("do_logout")).thenReturn("yes");
        
        servlet.doGet(request, response);
        
        verify(session).invalidate();
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader(matches("Location"), startsWith("http://someserver:1234/annotator/loggedout.html"));
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
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.localhost:1234/annotator/home"));
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("test.localhost");
        when(request.getServerPort()).thenReturn(1234);
        when(request.getContextPath()).thenReturn("annotator");
        
        servlet.doGet(request, response);
        
        assertTrue(StringUtils.isBlank(outputBuf.toString()));
        
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader(matches("Location"), startsWith("https://accounts.google.com/o/oauth2/auth"));
        verify(response).setHeader(matches("Location"), contains("test.localhost"));
        verify(response).setHeader(matches("Location"), contains("1234"));
    }
    
    //Technically this is a test for our Servlet base, but it's easier here
    @Test
    public void testGetNeedsLoginURLOverride() throws Exception {
        when(ctx.getString("annotator.test.user.email")).thenReturn("");
        when(ctx.getString("annotator.test.user.name")).thenReturn("");
        when(ctx.getString("annotator.test.user.notest")).thenReturn("true");
        
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("someserver");
        when(request.getServerPort()).thenReturn(1234);
        when(request.getContextPath()).thenReturn("annotator");
        
        when(ctx.getString("oauth2.google.auth.uri")).thenReturn("https://accounts.google.com/o/oauth2/auth");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        
        when(session.getAttribute("user.email")).thenReturn("");
        when(session.getAttribute("user.fullname")).thenReturn("");
        
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://my-test-server.testing.com:5678/annotator/home"));
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("my-test-server.testing.com");
        when(request.getServerPort()).thenReturn(5678);
        when(request.getContextPath()).thenReturn("annotator");
        
        servlet.doGet(request, response);
        
        assertTrue(StringUtils.isBlank(outputBuf.toString()));
        
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader(matches("Location"), startsWith("https://accounts.google.com/o/oauth2/auth"));
        verify(response).setHeader(matches("Location"), contains("my-test-server.testing.com"));
        verify(response).setHeader(matches("Location"), contains("5678"));
        verify(response, never()).setHeader(matches("Location"), contains("test.localhost"));
        verify(response, never()).setHeader(matches("Location"), contains("1234"));
    }
    
    @Test(expected=RuntimeException.class)
    public void testPost() throws Exception {
        MainServlet postWrapper = new MainServlet() {
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

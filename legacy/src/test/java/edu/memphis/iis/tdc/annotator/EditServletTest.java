package edu.memphis.iis.tdc.annotator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.config.SimpleEncrypt;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;
import edu.memphis.iis.tdc.annotator.model.Utterance;

public class EditServletTest {
    static String unhidden_fn;
    static String hidden_fn;
    
    final static String USR_EMAIL = "tester@test.iis.memphis.edu";
    final static String BASE_URL = "http://test.localhost:1234/annotator/edit";
    
    EditServlet servlet;
    
    ConfigContext ctx;
    TranscriptService tserv;
    TranscriptSession ts;
    HttpSession session;
    HttpServletRequest request;
    HttpServletResponse response;
    RequestDispatcher dispatcher;
    
    StringWriter outputBuf;
    
    @BeforeClass
    public static void staticSetUp() {
        unhidden_fn = "a_file.xml";
        hidden_fn = SimpleEncrypt.hideString(unhidden_fn);
    }
    
    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        
        servlet = new EditServlet();
        
        ctx = mock(ConfigContext.class);
        ConfigContext.overwriteCurrentInstance(ctx);
        
        tserv = mock(TranscriptService.class);
        when(ctx.getTranscriptService()).thenReturn(tserv);
        
        ts = mock(TranscriptSession.class);
        when(ts.getSourceFileName()).thenReturn(unhidden_fn);
        
        session = mock(HttpSession.class);
        
        request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURL()).thenReturn(new StringBuffer(BASE_URL));
        
        dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        
        outputBuf = new StringWriter();
        PrintWriter output = new PrintWriter(outputBuf);
        
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(output);
    }
    
    @After
    public void tearDown() throws Exception {
        validateMockitoUsage();
        ConfigContext.overwriteCurrentInstance(null);
        LogManager.resetConfiguration();
    }

    @Test
    public void testGet() throws Exception {
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(request.getQueryString()).thenReturn("state=InProgress&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("InProgress");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
        
        when(tserv.getSingleTranscript(State.InProgress, USR_EMAIL, unhidden_fn)).thenReturn(ts);
        
        servlet.doGet(request, response);
        
        verify(request).getRequestDispatcher("/WEB-INF/view/edit.jsp");
        verify(request).setAttribute("transcript", ts);
        verify(dispatcher).forward(request, response);
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
        when(request.getQueryString()).thenReturn("state=InProgress&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("InProgress");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("test.localhost");
        when(request.getServerPort()).thenReturn(1234);
        when(request.getContextPath()).thenReturn("annotator");
        
        servlet.doGet(request, response);
        
        assertTrue(StringUtils.isBlank(outputBuf.toString()));
        
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader(matches("Location"), startsWith("https://accounts.google.com/o/oauth2/auth"));
    }
    
    @Test
    public void testGetNoFile() throws Exception {
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(request.getQueryString()).thenReturn("state=InProgress&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("InProgress");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
                
        servlet.doGet(request, response);

        verify(request).getRequestDispatcher("/WEB-INF/view/error.jsp");
        verify(request).setAttribute(matches("errorMessage"), matches(".*(not).*(find).*(transcript).*"));
        verify(dispatcher).forward(request, response);
    }
    
    @Test
    public void testGetMovePending() throws Exception {
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(request.getQueryString()).thenReturn("state=Pending&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("Pending");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
        
        when(tserv.getSingleTranscript(State.Pending, USR_EMAIL, unhidden_fn)).thenReturn(ts);

        servlet.doGet(request, response);

        verify(tserv).moveTranscript(State.Pending, State.InProgress, USR_EMAIL, unhidden_fn);
        
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader("Location", BASE_URL + "?state=InProgress&fn=" + hidden_fn);
    }
        
    @Test
    public void testGetMovePendingFail() throws Exception {
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(request.getQueryString()).thenReturn("state=Pending&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("Pending");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
        
        when(tserv.getSingleTranscript(State.Pending, USR_EMAIL, unhidden_fn)).thenReturn(ts);
        
        doThrow(new IOException())
            .when(tserv).moveTranscript(
                    any(State.class), 
                    any(State.class), 
                    anyString(), 
                    anyString());
        
        servlet.doGet(request, response);
        
        verify(request).setAttribute(matches("errorMessage"), matches(".*([Pp]ending).*([Ii]n[Pp]rogress).*"));
        verify(request).getRequestDispatcher("/WEB-INF/view/error.jsp");
        verify(dispatcher).forward(request, response);
    }
    
    @Test
    public void testGetUnexpectedException() throws Exception {
        when(session.getAttribute("user.email"))
            .thenThrow(new IllegalArgumentException("Wakka Wakka!"));
        
        servlet.doGet(request, response);
        
        verify(request).setAttribute(matches("errorMessage"), matches(".*(Wakka Wakka).*"));
        verify(request).getRequestDispatcher("/WEB-INF/view/error.jsp");
        verify(dispatcher).forward(request, response);
    }

    //TODO: need a get on a transcript with a verify mode
    
    @Test
    public void testPostPendNotComp() throws Exception {
        //Yep - we'll test to see if a POST comes in so save a Pending
        //transcript.  Note that the "standard" UI would get it first,
        //but maybe we start supporting a non-standard client? 
        runStandardPost(State.Pending, false, "self");
        
        verify(tserv, never())
            .moveTranscript(any(State.class), any(State.class), anyString(), anyString());
    }
       
    @Test
    public void testPostInProgNotComp() throws Exception {
        runStandardPost(State.InProgress, false, "self");
        
        verify(tserv, never())
            .moveTranscript(any(State.class), any(State.class), anyString(), anyString());
    }
    
    @Test
    public void testPostCompNotComp() throws Exception {
        runStandardPost(State.Completed, false, "self");
        
        verify(tserv, never())
            .moveTranscript(any(State.class), any(State.class), anyString(), anyString());
    }
    
    @Test
    public void testPostCompComp() throws Exception {
        runStandardPost(State.Completed, true, "self");
        
        verify(tserv, never())
            .moveTranscript(any(State.class), any(State.class), anyString(), anyString());
    }
    
    @Test
    public void testPostBlankTopLevelSucceeds() throws Exception {
        runStandardPost(State.InProgress, false, "self", "", "", "", "");
        
        verify(tserv, never())
            .moveTranscript(any(State.class), any(State.class), anyString(), anyString());
    }
    
    private void runStandardPost(State s, boolean isComp, String redirTest) throws Exception {
        runStandardPost(s, isComp, redirTest, "3", "4", "LA Comments", "Sess Comments");
    }
    private void runStandardPost(
    		State s, 
    		boolean isComp, 
    		String redirTest, 
    		String soundness,
    		String learningAssessmentScore,
    		String learningAssessmentComments,
    		String sessionComments) 
    		throws Exception 
    {      
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(request.getQueryString()).thenReturn("state=" + s.toString() + "&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn(s.toString());
        when(request.getParameter("fn")).thenReturn(hidden_fn);

        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(ts.isTaggingComplete()).thenReturn(isComp);
        
        when(tserv.getSingleTranscript(s, USR_EMAIL, unhidden_fn)).thenReturn(ts);
        
        StringBuilder fulldata = new StringBuilder();
        fulldata.append("[");
        fulldata.append("{\"act\":\"a1\", \"subact\":\"sa1\", \"mode\":\"m1\", \"comments\":\"hello\", \"confidence\":\"1\", \"index\":0}");
        fulldata.append(",");
        fulldata.append("{\"act\":\"a2\", \"subact\":\"sa2\", \"mode\":\"m2\", \"comments\":\"world\", \"confidence\":\"0\", \"index\":1}");
        fulldata.append("]");
        
        when(request.getParameter("fulldata")).thenReturn(fulldata.toString());
        when(request.getParameter("soundness")).thenReturn(soundness);
        when(request.getParameter("learningAssessmentScore")).thenReturn(learningAssessmentScore);
        when(request.getParameter("learningAssessmentComments")).thenReturn(learningAssessmentComments);
        when(request.getParameter("sessionComments")).thenReturn(sessionComments);
        
        Utterance u1 = mock(Utterance.class);
        Utterance u2 = mock(Utterance.class);
        List<Utterance> utts = new ArrayList<Utterance>();
        utts.add(u1);
        utts.add(u2);
        when(ts.getTranscriptItems()).thenReturn(utts);
        
        servlet.doPost(request, response);
        
        verify(ts).setSoundness(soundness);
        verify(ts).setLearningAssessmentScore(learningAssessmentScore);
        verify(ts).setLearningAssessmentComments(learningAssessmentComments);
        verify(ts).setSessionComments(sessionComments);
        
        verify(u1).setDialogAct("a1");
        verify(u1).setDialogSubAct("sa1");
        verify(u1).setDialogMode("m1");
        verify(u1).setComments("hello");
        
        verify(u2).setDialogAct("a2");
        verify(u2).setDialogSubAct("sa2");
        verify(u2).setDialogMode("m2");
        verify(u2).setComments("world");
        
        verify(tserv).writeTranscript(ts, s, USR_EMAIL, unhidden_fn);
        
        //Do they want us to check that the redir happened?
        if (redirTest != null) {
            verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
            
            if ("home".equals(redirTest)) {
                verify(response).setHeader("Location", "home");
            }
            else if ("self".equals(redirTest)) {
                verify(response).setHeader("Location", BASE_URL + "?state=" + s.toString() + "&fn=" + hidden_fn);
            }
            else {
                fail("Unknown redir test request");
            }
        }
    }
    
    @Test
    public void testPostWriteFailed() throws Exception {
        doThrow(new IOException())
            .when(tserv).writeTranscript(ts, State.InProgress, USR_EMAIL, unhidden_fn);
        
        runStandardPost(State.InProgress, false, null);
        
        verify(request).getRequestDispatcher("/WEB-INF/view/error.jsp");
        verify(request).setAttribute(matches("errorMessage"), matches(".*(not).*(write).*(file).*"));
        verify(dispatcher).forward(request, response);
    }
    
    @Test
    public void testPostWriteUnexpecedException() throws Exception {
        //Make something happen that is completely unexpected.  Note this is
        //really more of a test of the ServletBase, but it's easier to do from
        //here since this is our most intense servlet
        doThrow(new IllegalArgumentException("Mwah ha ha"))
            .when(tserv).writeTranscript(ts, State.InProgress, USR_EMAIL, unhidden_fn);
        
        runStandardPost(State.InProgress, false, null);
        
        verify(request).getRequestDispatcher("/WEB-INF/view/error.jsp");
        verify(request).setAttribute(matches("errorMessage"), matches(".*(Mwah ha ha).*"));
        verify(dispatcher).forward(request, response);
    }
        
    @Test
    public void testPostNeedsLogin() throws Exception {     
        when(ctx.getString("annotator.test.user.email")).thenReturn("");
        when(ctx.getString("annotator.test.user.name")).thenReturn("");
        when(ctx.getString("annotator.test.user.notest")).thenReturn("true");
        
        when(ctx.getString("oauth2.google.auth.uri")).thenReturn("https://accounts.google.com/o/oauth2/auth");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        
        when(session.getAttribute("user.email")).thenReturn("");
        when(session.getAttribute("user.fullname")).thenReturn("");
        
        when(request.getSession(true)).thenReturn(session);
        when(request.getQueryString()).thenReturn("state=InProgress&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("InProgress");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("test.localhost");
        when(request.getServerPort()).thenReturn(1234);
        when(request.getContextPath()).thenReturn("annotator");
        
        //POST shouldn't do any reads or writes if they need to login
        when(tserv.getSingleTranscript(any(State.class), anyString(), anyString()))
            .thenThrow(new RuntimeException());
        
        doThrow(new RuntimeException()).
            when(tserv).writeTranscript(
                    any(TranscriptSession.class), 
                    any(State.class), 
                    anyString(), 
                    anyString());
        
        doThrow(new RuntimeException())
            .when(tserv).moveTranscript(
                    any(State.class), 
                    any(State.class), 
                    anyString(), 
                    anyString());
        
        servlet.doPost(request, response);
        
        assertTrue(StringUtils.isBlank(outputBuf.toString()));
        
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader(matches("Location"), startsWith("https://accounts.google.com/o/oauth2/auth"));
    }
    
    @Test
    public void testPostNoFile() throws Exception {
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(request.getQueryString()).thenReturn("state=InProgress&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("InProgress");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
        
        servlet.doPost(request, response);

        verify(request).getRequestDispatcher("/WEB-INF/view/error.jsp");
        verify(request).setAttribute(matches("errorMessage"), matches(".*(not).*(find).*(transcript).*"));
        verify(dispatcher).forward(request, response);
    }
    
    @Test
    public void testPostNoRawData() throws Exception {
        when(session.getAttribute("user.email")).thenReturn(USR_EMAIL);
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        when(request.getQueryString()).thenReturn("state=InProgress&fn=" + hidden_fn);
        when(request.getParameter("state")).thenReturn("InProgress");
        when(request.getParameter("fn")).thenReturn(hidden_fn);
        
        when(tserv.getSingleTranscript(State.InProgress, USR_EMAIL, unhidden_fn)).thenReturn(ts);
        
        //No fulldata - they will just redirect to the GET
        when(request.getParameter("fulldata")).thenReturn("");
        
        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader("Location", BASE_URL + "?state=InProgress&fn=" + hidden_fn);
    }   
}

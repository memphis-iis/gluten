package edu.memphis.iis.tdc.annotator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.model.Taxonomy;

public class ServletBaseTest {
    private static class ServletBaseImpl extends ServletBase {
        private static final long serialVersionUID = 1L;
        
        @Override
        protected String doProtectedGet(HttpServletRequest request, HttpServletResponse response) 
                throws ServletException, IOException, UserErrorException 
        {
            return NO_VIEW;
        }

        @Override
        protected String doProtectedPost(HttpServletRequest request, HttpServletResponse response) 
                throws ServletException, IOException, UserErrorException 
        {
            return NO_VIEW;
        }
    }
    
    private ServletBaseImpl servlet = null;
    
    @Before
    public void setUp() {
        ConfigContext.overwriteCurrentInstance(null);
        servlet = new ServletBaseImpl();
    }
    
    @After
    public void tearDown() {
        validateMockitoUsage();
        ConfigContext.overwriteCurrentInstance(null);
    }
    
    @Test
    public void testUserInfoInTest() {
        ConfigContext ctx = mock(ConfigContext.class);
        when(ctx.getString("annotator.test.user.email")).thenReturn("cnkelly@memphis.edu");
        when(ctx.getString("annotator.test.user.name")).thenReturn("Craig Kelly (TEST FILE)");
        ConfigContext.overwriteCurrentInstance(ctx);
        
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user.email")).thenReturn("");
        when(session.getAttribute("user.fullname")).thenReturn("");
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        
        //We are testing that src/test/resources/annotator.properties
        //test user info is showing up (and that we're not getting
        //forced to redirect)
        assertEquals("cnkelly@memphis.edu", servlet.getUserEmail(request));
        assertEquals("Craig Kelly (TEST FILE)", servlet.getUserFullName(request));
        assertEquals("cnkelly@memphis.edu", servlet.getUserForLogging(request));
    }
    
    @Test
    public void testUserInfoInSession() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user.email")).thenReturn("test.email@somewhere.com");
        when(session.getAttribute("user.fullname")).thenReturn("Fozzy Bear");
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        
        //We are testing that a properly setup session returns the correct user cred's
        assertEquals("test.email@somewhere.com", servlet.getUserEmail(request));
        assertEquals("Fozzy Bear", servlet.getUserFullName(request));
        assertEquals("test.email@somewhere.com", servlet.getUserForLogging(request));
    }
    
    @Test
    public void testRequestParmsSetupNobody() throws Exception {
        checkRequestParms("tester@test.com", "assigner@test.com", "verifier@test.com", false, false);
    }
    
    @Test
    public void testRequestParmsSetupAssigner() throws Exception {
        checkRequestParms("assigner@test.com", "assigner@test.com", "verifier@test.com", true, false);
    }
    
    @Test
    public void testRequestParmsSetupVerifier() throws Exception {
        checkRequestParms("verifier@test.com", "assigner@test.com", "verifier@test.com", false, true);
    }
    
    @Test
    public void testRequestParmsSetupSomebody() throws Exception {
        checkRequestParms("fromage@test.com", "fromage@test.com", "fromage@test.com", true, true);        
    }
    
    private void checkRequestParms(
            String usrEmail, 
            String assignerEmail, 
            String verifierEmail, 
            boolean isAssigner, 
            boolean isVerifier)
                    throws Exception
    {        
        ConfigContext ctx = mock(ConfigContext.class);
        Taxonomy tax = mock(Taxonomy.class);
        when(ctx.getTaxonomy()).thenReturn(tax);
        
        when(ctx.userIsAssigner(assignerEmail)).thenReturn(true);
        when(ctx.userIsVerifier(verifierEmail)).thenReturn(true);
        
        ConfigContext.overwriteCurrentInstance(ctx);
        
        HttpSession session = mock(HttpSession.class);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://somewhere.com:1234/annotator/nowhere"));
        when(request.getQueryString()).thenReturn("a=b");
                
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        final String TEST_LOC = "my-loc-is-en";
        final String TEST_PHOTO = "img/myimage.jpg";
        
        when(session.getAttribute(Const.SESS_USR_EMAIL)).thenReturn(usrEmail);
        when(session.getAttribute(Const.SESS_USR_NAME)).thenReturn("Fozzy Bear");
        when(session.getAttribute(Const.SESS_USR_LOC)).thenReturn(TEST_LOC);
        when(session.getAttribute(Const.SESS_USR_PHOTO)).thenReturn(TEST_PHOTO);
        
        servlet.doGet(request, response);
        
        verify(request).setAttribute(Const.REQ_USR_NAME, "Fozzy Bear");
        verify(request).setAttribute(Const.REQ_USR_EMAIL, usrEmail);
        verify(request).setAttribute("requestURL", "http://somewhere.com:1234/annotator/nowhere?a=b");
        verify(request).setAttribute("taxonomy", tax);
        verify(request).setAttribute(Const.REQ_USR_LOC, TEST_LOC);
        verify(request).setAttribute(Const.REQ_USR_PHOTO, TEST_PHOTO);
        
        verify(request).setAttribute(Const.REQ_IS_ASSIGNER, isAssigner);
        verify(request).setAttribute(Const.REQ_IS_VERIFIER, isVerifier);        
    }
}

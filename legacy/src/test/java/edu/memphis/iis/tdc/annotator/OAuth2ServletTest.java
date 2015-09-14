package edu.memphis.iis.tdc.annotator;

//import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.data.HttpClient;

public class OAuth2ServletTest {    
    OAuth2Servlet servlet;
    
    ConfigContext ctx;
    HttpSession session;
    HttpServletRequest request;
    HttpServletResponse response;
    HttpClient httpClient;
    
    StringWriter outputBuf;
    
    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        
        servlet = new OAuth2Servlet();

        ctx = mock(ConfigContext.class);
        ConfigContext.overwriteCurrentInstance(ctx);
        
        httpClient = mock(HttpClient.class);
        when(ctx.getHttpClient()).thenReturn(httpClient);
        
        session = mock(HttpSession.class);
        when(session.getAttribute("user.email")).thenReturn("");
        when(session.getAttribute("user.fullname")).thenReturn("");
        
        request = mock(HttpServletRequest.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.localhost:1234/annotator/oauth2callback"));
        
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
    public void testGetVanilla() throws Exception {
        doGetTest("a", "b", "c", "d");
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader("Location", "home");
    }
    
    @Test
    public void testGetOnlyEmail() throws Exception {
        doGetTest("tester@test.com", "", "", "");
        verify(response).setStatus(HttpServletResponse.SC_SEE_OTHER);
        verify(response).setHeader("Location", "home");
    }
    
    @Test
    public void testGetWithBadEmail() throws Exception {
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/view/error.jsp")).thenReturn(dispatcher);
        
        doGetTest("", "", "", "");
        verify(request).setAttribute(matches("errorMessage"), matches(".*(email).*(not).*(locate).*"));
        verify(dispatcher).forward(request, response);
    }
    
    private void doGetTest(String email, String name, String loc, String photo) throws Exception {
        when(ctx.getString("oauth2.google.token.uri")).thenReturn("https://accounts.google.com/o/oauth2/token");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.client.secret")).thenReturn("super-secret-client-secret");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        when(ctx.getString("oauth2.google.userinfo.uri")).thenReturn("https://www.googleapis.com/oauth2/v2/userinfo");
        
        when(request.getParameter("code")).thenReturn("code-ok");
        
        String accessJson = "{\"access_token\": \"ACCESST\", \"refresh_token\": \"REFRESHT\"}";
        
        StringBuilder userInfoJson = new StringBuilder();
        userInfoJson.append("{");
        userInfoJson.append("\"email\": \"" + email + "\",");
        userInfoJson.append("\"name\": \"" + name + "\",");
        userInfoJson.append("\"locale\": \"" + loc + "\",");
        userInfoJson.append("\"picture\": \"" + photo + "\"");
        userInfoJson.append("}");
        
        when(httpClient.execute(any(HttpRequestBase.class)))
            .thenReturn(accessJson)
            .thenReturn(userInfoJson.toString());
        
        servlet.doGet(request, response);
        
        verify(session).setAttribute(Const.SESS_OAUTH_ACCESS_TOK, "ACCESST");
        verify(session).setAttribute(Const.SESS_OAUTH_REFRESH_TOK, "REFRESHT");
        
        verify(session).setAttribute(Const.SESS_USR_EMAIL, email);
        verify(session).setAttribute(Const.SESS_USR_NAME, name);
        verify(session).setAttribute(Const.SESS_USR_LOC, loc);
        verify(session).setAttribute(Const.SESS_USR_PHOTO, photo);
    }
    
    @Test
    public void testGetWithErrMsg() throws Exception {
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        
        when(request.getParameter("error")).thenReturn("Fozzy Wuz Here");
        when(request.getRequestDispatcher("/WEB-INF/view/error.jsp")).thenReturn(dispatcher);
        
        servlet.doGet(request, response);

        verify(request).setAttribute("errorMessage", "Fozzy Wuz Here");
        verify(dispatcher).forward(request, response);
    }
    
    @Test
    public void testGetBadCode() throws Exception {
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        
        when(request.getParameter("code")).thenReturn("");
        when(request.getRequestDispatcher("/WEB-INF/view/error.jsp")).thenReturn(dispatcher);
        
        servlet.doGet(request, response);

        verify(request).setAttribute(matches("errorMessage"), matches(".*([Nn]o).*(code).*"));
        verify(dispatcher).forward(request, response);
    }
    
    @Test
    public void testGetBadFirstReadClient() throws Exception {
        when(ctx.getString("oauth2.google.token.uri")).thenReturn("https://accounts.google.com/o/oauth2/token");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.client.secret")).thenReturn("super-secret-client-secret");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        
        when(request.getParameter("code")).thenReturn("code-ok");
        when(request.getRequestDispatcher("/WEB-INF/view/error.jsp")).thenReturn(dispatcher);
        
        when(httpClient.execute(any(HttpRequestBase.class))).thenThrow(new UserErrorException("Blammo"));
        
        servlet.doGet(request, response);

        verify(request).setAttribute(matches("errorMessage"), matches(".*([Ee]rror).*(Blammo).*"));
        verify(dispatcher).forward(request, response);      
    }
    
    @Test
    public void testGetBadFirstReadURL() throws Exception {
        //Note our bad URI to force the correct exception
        when(ctx.getString("oauth2.google.token.uri")).thenReturn("ht\u0012tps://accounts.google.com/o/oauth2/token");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.client.secret")).thenReturn("super-secret-client-secret");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        
        when(request.getParameter("code")).thenReturn("code-ok");
        when(request.getRequestDispatcher("/WEB-INF/view/error.jsp")).thenReturn(dispatcher);
        
        when(httpClient.execute(any(HttpRequestBase.class))).thenThrow(new UserErrorException("Blammo"));
        
        servlet.doGet(request, response);

        verify(request).setAttribute(matches("errorMessage"), matches(".*(URI).*"));
        verify(dispatcher).forward(request, response);      
    }
    
    @Test
    public void testGetBadFirstReadJSON() throws Exception {
        when(ctx.getString("oauth2.google.token.uri")).thenReturn("https://accounts.google.com/o/oauth2/token");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.client.secret")).thenReturn("super-secret-client-secret");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        
        when(request.getParameter("code")).thenReturn("code-ok");
        when(request.getRequestDispatcher("/WEB-INF/view/error.jsp")).thenReturn(dispatcher);
        
        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn("This is invalid JSON!");
        
        servlet.doGet(request, response);

        verify(request).setAttribute(matches("errorMessage"), matches(".*(parsing).*"));
        verify(dispatcher).forward(request, response);      
    }
    
    @Test
    public void testGetBadFirstReadBlankAccess() throws Exception {
        when(ctx.getString("oauth2.google.token.uri")).thenReturn("https://accounts.google.com/o/oauth2/token");
        when(ctx.getString("oauth2.google.client.id")).thenReturn("super-special-client-id");
        when(ctx.getString("oauth2.google.client.secret")).thenReturn("super-secret-client-secret");
        when(ctx.getString("oauth2.google.scope")).thenReturn("da-scope");
        
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        
        when(request.getParameter("code")).thenReturn("code-ok");
        when(request.getRequestDispatcher("/WEB-INF/view/error.jsp")).thenReturn(dispatcher);
        
        String accessJson = "{\"access_token\": \"\", \"refresh_token\": \"REFRESHT\"}";
        when(httpClient.execute(any(HttpRequestBase.class))).thenReturn(accessJson);
        
        servlet.doGet(request, response);

        verify(request).setAttribute(matches("errorMessage"), matches(".*(no).*(access).*"));
        verify(dispatcher).forward(request, response);      
    }
}

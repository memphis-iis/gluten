package edu.memphis.iis.tdc.annotator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;
import edu.memphis.iis.tdc.annotator.model.Utterance;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Helper base class for our simple servlets.  Provides:
 * <ul>
 *   <li>Logging help
 *   <li>Wraps doGet and doPost to require secured login
 *   <li>Provides current user ID
 * </ul>
 * 
 * <p>Note that we use Google OAuth 2, so only the first part of the login
 * is handled here.  We give Google a redirect URL that should point to
 * our OAuth2Servlet servlet
 */
public abstract class ServletBase extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected transient Logger logger;
    
    public static final String NO_VIEW = "{{NOVIEW}}";
    
    /**
     * Direct access to a Logger instance for the current class
     */
    //Suppress FindBugs - the warning is spurious 
    @SuppressWarnings
    public synchronized Logger log() {
        if (logger == null) {
            logger = Logger.getLogger(this.getClass());
        }
        return logger;
    }
    
    /**
     * Helper for writing to the audit log - this is meant to work in
     * tandem with a log4j config.  The message is logged at the INFO level
     */
    public void audit(String msg) {
        audit(Level.INFO, msg);
    }
    
    /**
     * Helper for writing to the audit log - this is meant to work in
     * tandem with a log4j config.  The level logged to is exposed.  This
     * can be helpful with a log4j config that has both an error log and
     * and audit log
     */
    public void audit(Level level, String msg) {
        log().log(level, "[[AUDIT]] " + msg);
    }
    
    /**
     * Return the current user's email address as stored in the session.  If
     * it is not in the session, then a test value will be used from the
     * property file.  If that is not there, then an empty string will be
     * returned.  Note the assumption that something else will force login
     */
    public String getUserEmail(HttpServletRequest request) {
        String email = (String)request.getSession(true).getAttribute(Const.SESS_USR_EMAIL);
        if (StringUtils.isBlank(email)) {
            email = ConfigContext.getInst().getString(Const.PROP_TEST_USR_EMAIL);
        }
        if (StringUtils.isBlank(email)) {
            email = "";
        }
        return email;
    }
    
    public String getUserFullName(HttpServletRequest request) {
        String fullName = (String)request.getSession(true).getAttribute(Const.SESS_USR_NAME);
        if (StringUtils.isBlank(fullName)) {
            fullName = ConfigContext.getInst().getString(Const.PROP_TEST_USR_NAME);
        }
        if (StringUtils.isBlank(fullName)) {
            fullName = "";
        }
        return fullName;
    }
    
    //Sometimes we need a string to log regardless of whether or not
    //someone if logged in - and DON'T leak or log exceptions
    protected String getUserForLogging(HttpServletRequest request) {
        String user;
        try {
            user = getUserEmail(request);
        }
        catch(Throwable t) {
            user = "";
        }
        
        if (StringUtils.isBlank(user))
            user = "[Anonymous User]";
        return user;
    }
    
    protected boolean isUserAssigner(HttpServletRequest request) {
        return ConfigContext.getInst().userIsAssigner(getUserEmail(request));
    }
    
    protected boolean isUserVerifier(HttpServletRequest request) {
        return ConfigContext.getInst().userIsVerifier(getUserEmail(request));
    }
    
    protected abstract String doProtectedGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, UserErrorException;
    
    protected abstract String doProtectedPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException
    {
        try {
            if (!setupHttpContext(request, response)) {
                return; //response has been handled
            }
            String target = doProtectedGet(request, response);
            handleHttpTarget(request, response, target);
        }
        catch (UserErrorException e) {
            log().error("User error message on GET - sending back to user " + getUserForLogging(request), e);
            doErrorPage(request, response, e.getMessage());
        }
        catch(Throwable t) {
            //They threw something other than a user exception.  We'll
            //let a ServletException pass, but everything else get's
            //logged and handled as an error page
            if (t instanceof ServletException) {
                throw t;
            }
            log().error("UNEXPECTED EXCEPTION on GET for user " + getUserForLogging(request), t);
            doErrorPage(request, response, t.getMessage());
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        try {
            if (!setupHttpContext(request, response)) {
                return; //response has been handled
            }
            String target = doProtectedPost(request, response);
            handleHttpTarget(request, response, target);
        }
        catch (UserErrorException e) {
            log().error("User error message on POST - sending back to user " + getUserForLogging(request), e);
            doErrorPage(request, response, e.getMessage());
        }
        catch(Throwable t) {
            //They threw something other than a user exception.  We'll
            //let a ServletException pass, but everything else get's
            //logged and handled as an error page
            if (t instanceof ServletException) {
                throw t;
            }
            log().error("UNEXPECTED EXCEPTION on POST for user " + getUserForLogging(request), t);
            doErrorPage(request, response, t.getMessage());
        }
    }
    
    /**
     * Returns false if the context fails.  If we DO return false, then our
     * caller should assume that the response has been changed (error msg,
     * redirect, etc) and processing should stop
     */
    private boolean setupHttpContext(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        String userEmail = getUserEmail(request);
        if (StringUtils.isBlank(userEmail)) {
            //No user specified - we need to force a login
            seeLogin(request, response);
            return false;
        }
        
        //Admin roles
        boolean assigner = isUserAssigner(request);
        boolean verifier = isUserVerifier(request);
        
        //REQUIRED to be there
        request.setAttribute(Const.REQ_USR_NAME, getUserFullName(request));
        request.setAttribute(Const.REQ_USR_EMAIL, userEmail);
        request.setAttribute("requestURL", getFullURL(request));
        request.setAttribute("taxonomy", ConfigContext.getInst().getTaxonomy());
        request.setAttribute(Const.REQ_IS_ASSIGNER, assigner);
        request.setAttribute(Const.REQ_IS_VERIFIER, verifier);
        request.setAttribute(Const.REQ_IS_ASSESSOR, assigner || verifier);
        
        //Things we provide as a nicety if they were found (and are generally
        //set by the login) note that for a few, we actually handle a default       
        HttpSession session = request.getSession(true);
        
        request.setAttribute(Const.REQ_USR_LOC, 
                getOptStr(session, Const.SESS_USR_LOC, "en"));
        
        request.setAttribute(Const.REQ_USR_PHOTO, 
                getOptStr(session, Const.SESS_USR_PHOTO, "img/anonymous_person.png"));
        
        return true;
    }
    
    //Return the session string variable.  If isn't there, set the session
    //variable to the default value and use that
    private String getOptStr(HttpSession session, String name, String defVal) {
        String val = (String)session.getAttribute(name);
        if (StringUtils.isBlank(val)) {
            val = defVal;
            session.setAttribute(name, val);
        }
        return val;
    }
    
    private void handleHttpTarget(
            HttpServletRequest request, 
            HttpServletResponse response, 
            String target) 
            throws UserErrorException, ServletException, IOException 
    {
        if (StringUtils.isBlank(target)) {
            throw new UserErrorException("No view specified for the method");
        }
        else if (NO_VIEW.equals(target)) {
            return;
        }
        
        request.getRequestDispatcher(target).forward(request, response);
    }
    
    /**
     * Return the full URL (with query string) for the specified request
     */
    protected String getFullURL(HttpServletRequest request) {
        String qs = request.getQueryString();
        qs = StringUtils.isBlank(qs) ? "" : "?"+qs;
        return request.getRequestURL() + qs;
    }
    
    /**
     * Wrapper (with logging) around static rawBuildAppURL
     */
    protected String buildAppURL(HttpServletRequest request, String component) {
        URI uri;
        try {
            uri = rawBuildAppURL(request, component);
        }
        catch (URISyntaxException e) {
            log().warn("Syntax error building a URI for req " + request.getRequestURI(), e);
            return null;
        }
        
        return uri.toString();
    }
    
    /**
     * Construct a link to the given component for this app
     * For instance, if this app is at http://localhost:8080/annotator, then
     * <code>buildAppURL(request, "xyz")</code> should return
     * http://localhost:8080/annotator/xyz
     * 
     * <p>Mainly called by the method buildAppURL
     */
    public static URI rawBuildAppURL(HttpServletRequest request, String component) throws URISyntaxException {
        return new URIBuilder()
            .setScheme(request.getScheme())
            .setHost(request.getServerName())
            .setPort(request.getServerPort())
            .setPath("/" + request.getContextPath() + "/" + component)
            .build();
    }
    
    protected void doErrorPage(HttpServletRequest request, HttpServletResponse response, String msg) 
            throws ServletException, IOException 
    {
        request.setAttribute(Const.REQ_ERR_MSG, msg);
        request.getRequestDispatcher("/WEB-INF/view/error.jsp").forward(request, response);
    }
    
    protected void seeOther(HttpServletRequest request, HttpServletResponse response, String url) {
        log().info(String.format("Redirecting %s to %s", getUserEmail(request), url));
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", url);
    }
    
    //Start them on the login path - note that we're currently using
    //only Google
    protected void seeLogin(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        try {
            ConfigContext ctx = ConfigContext.getInst();
            
            //Note changes you may need:
            // * access_type should be "offline" if we want a refresh token to
            //   come back with the access token
            // * approval_prompt should be "force" if we want them to see accept
            //   the login every time
            URI uri = new URIBuilder(ctx.getString("oauth2.google.auth.uri"))
                .setParameter("client_id", ctx.getString("oauth2.google.client.id"))
                .setParameter("response_type", "code")
                .setParameter("scope", ctx.getString("oauth2.google.scope"))
                .setParameter("redirect_uri", buildAppURL(request, "oauth2callback"))
                .setParameter("state", Long.toString(Thread.currentThread().getId()))
                .setParameter("access_type", "online")
                .setParameter("approval_prompt", "auto")
                .build();
            
            seeOther(request, response, uri.toString());
            
        }
        catch (Exception e) {
            log().error("There was an error attempting to start login process!", e);
            doErrorPage(request, response, e.getMessage()); 
        }
    }
    
    protected void userAudit(
            String auditMsg, 
            HttpServletRequest request, 
            State state, 
            String baseFileName, 
            TranscriptSession ts) 
    {
        userAudit(Level.INFO, auditMsg, request, state, baseFileName, ts);
    }
    
    protected void userAudit(
            Level level, 
            String auditMsg, 
            HttpServletRequest request, 
            State state, 
            String baseFileName, 
            TranscriptSession ts) 
    {       
        
        String errMsg = String.format("%s: {usr:%s,name:%s,state:%s,baseFn:%s,srcFn:%s}",
                auditMsg,
                getUserEmail(request), 
                getUserFullName(request),
                state == null ? "" : state.toString(),
                baseFileName,
                ts.getSourceFileName());
        
        int act = 0;
        int subact = 0;
        int mode = 0;
        
        for(Utterance u: ts.getTranscriptItems()) {
            act += validTag(u.getDialogAct());
            subact += validTag(u.getDialogSubAct());
            mode += validTag(u.getDialogMode());
        }
        
        errMsg += String.format(" {totitems:%d,act:%d,subact:%d,mode:%d}",
            ts.getTranscriptItems().size(),
            act,
            subact,
            mode);
        
        audit(level, errMsg);
    }
    
    private static int validTag(String s) {
        if (StringUtils.isBlank(s) || "unspecified".equals(s.toLowerCase(Locale.ENGLISH))) {
            return 0;
        }
        else {
            return 1;
        }
    }
    
    /**
     * Helper for parsing state string submitted to a servlet
     */
    protected TranscriptService.State parseState(String state) throws UserErrorException {
        if (StringUtils.isBlank(state))
            throw new UserErrorException("No file state specified");
        
        try {
            return TranscriptService.State.valueOf(state);
        }
        catch(Throwable t) {
            throw new UserErrorException("Unknown file state specificed: " + state, t);
        }
    }
}

package edu.memphis.iis.tdc.annotator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.data.HttpClient;

/**
 * Provides the callback endpoint for OAuth2 callbacks. Note that unlike our
 * other servlets, this one does NOT extend our ServletBase class. It works
 * "outside" the normal security and other requirements.
 */
@WebServlet(value="/oauth2callback", loadOnStartup=2)
public class OAuth2Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(OAuth2Servlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        //OK, in theory we redirected to google, the user did stuff, and now
        //google has redirected them here.  Time to figure out what went down

        // if the user denied access, we get back an error, ex
        // error=access_denied&state=session%3Dpotatoes

        String serverError = request.getParameter("error");
        if (StringUtils.isNotBlank(serverError)) {
            doErrorPage(request, response, serverError, null);
            return;
        }

        // google returns a code that can be exchanged for an access token
        String code = request.getParameter("code");
        if (StringUtils.isBlank(code)) {
            doErrorPage(request, response, "No access code given by the remote server", null);
            return;
        }

        ConfigContext ctx = ConfigContext.getInst();
        HttpClient http = ctx.getHttpClient();

        //Exchange the code for an access token
        //We should get back a JSON response with the fields: access_token,
        //token_type, expires_in, id_token, and refresh_token
        String accessTokenMsgRaw;
        try {
            URI getTokenUri = new URIBuilder(ctx.getString("oauth2.google.token.uri"))
                .build();

            URI redirect = ServletBase.rawBuildAppURL(request, "oauth2callback");

            List<NameValuePair> form = new ArrayList<NameValuePair>();
            form.add(new BasicNameValuePair("code", code));
            form.add(new BasicNameValuePair("client_id", ctx.getString("oauth2.google.client.id")));
            form.add(new BasicNameValuePair("client_secret", ctx.getString("oauth2.google.client.secret")));
            form.add(new BasicNameValuePair("redirect_uri", redirect.toString()));
            form.add(new BasicNameValuePair("grant_type", "authorization_code"));

            HttpPost getTokenPost = new HttpPost(getTokenUri);
            getTokenPost.setEntity(new UrlEncodedFormEntity(form));

            accessTokenMsgRaw = http.execute(getTokenPost);
        }
        catch (URISyntaxException e) {
            doErrorPage(request, response, "Could not format URI for access token", e);
            return;
        }
        catch(UserErrorException e) {
            doErrorPage(request, response, "There was an error getting the access token", e);
            return;
        }

        //Now parse the result we got back
        JSONObject accessTokenMsg = null;
        try {
            accessTokenMsg = new JSONObject(accessTokenMsgRaw);
        }
        catch (Exception e) {
            doErrorPage(request, response, "There was an issue parsing the remote server access token response", e);
            return;
        }

        String accessToken = null;
        String refreshToken = null;

        try {
            //If the google access token expires, then we can refresh it
            //with the refresh token (if we have it, which we currently
            //probably won't)
            accessToken = accessTokenMsg.getString("access_token");
            refreshToken = accessTokenMsg.optString("refresh_token", "");
        }
        catch(Exception e) {
            doErrorPage(request, response, "There was an issue extracting data from the remote server access token response", e);
        }

        //MUST have an access token
        if (StringUtils.isBlank(accessToken)) {
            doErrorPage(request, response, "There was no access token found at the remote server", null);
            return;
        }
        //But we can get along without a refresh token
        if (StringUtils.isBlank(refreshToken)) {
            refreshToken = "";
        }

        //Start putting stuff in the session
        HttpSession session = request.getSession(true);
        session.setAttribute(Const.SESS_OAUTH_ACCESS_TOK, accessToken);
        session.setAttribute(Const.SESS_OAUTH_REFRESH_TOK, refreshToken);

        //FINALLY we can get some info about the user with the access token
        String userInfoMsgRaw;
        try {
            URI getInfoUri = new URIBuilder(ctx.getString("oauth2.google.userinfo.uri"))
                .setParameter("access_token", accessToken)
                .build();

            HttpGet getInfoGet = new HttpGet(getInfoUri);
            getInfoGet.addHeader("Authorization", "Bearer " + accessToken);

            userInfoMsgRaw = http.execute(getInfoGet);
        }
        catch (URISyntaxException e) {
            doErrorPage(request, response, "Could not format URI for access token", e);
            return;
        }
        catch(UserErrorException e) {
            doErrorPage(request, response, "There was an error getting the access token", e);
            return;
        }

        //Parse the results
        JSONObject userInfoObj = new JSONObject(userInfoMsgRaw);
        //Note that we ALWAYS lower case the email address
        String userEmail = userInfoObj.optString("email", "").trim().toLowerCase(Locale.ENGLISH);

        //Save everything to the session
        session.setAttribute(Const.SESS_USR_EMAIL, userEmail);
        session.setAttribute(Const.SESS_USR_NAME, userInfoObj.optString("name", userEmail));
        session.setAttribute(Const.SESS_USR_LOC, userInfoObj.optString("locale", "en"));
        session.setAttribute(Const.SESS_USR_PHOTO, userInfoObj.optString("picture", "img/anonymous_person.png"));

        //NOW show an error if we couldn't get their email
        if (StringUtils.isBlank(userEmail)) {
            doErrorPage(request, response, "Your email address could not be located", null);
            return;
        }

        //Made it - they can go home now
        logger.info(String.format("Login for %s OK - Redirecting to home", userEmail));
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", "home");
    }

    //Taken from ServletBase
    protected void doErrorPage(HttpServletRequest request, HttpServletResponse response, String msg, Throwable t)
            throws ServletException, IOException
    {
        String finalMsg;
        if (t == null) {
            logger.warn(msg);
            finalMsg = msg;
        }
        else {
            logger.warn(msg, t);
            finalMsg = msg + " (" + t.getMessage() + ")";
        }
        request.setAttribute("errorMessage", finalMsg);
        request.getRequestDispatcher("/WEB-INF/view/error.jsp").forward(request, response);
    }
}

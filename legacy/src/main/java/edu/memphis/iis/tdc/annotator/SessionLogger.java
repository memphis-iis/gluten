package edu.memphis.iis.tdc.annotator;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

@WebListener
public class SessionLogger implements HttpSessionListener {
    private Logger log() {
        return Logger.getLogger(SessionLogger.class);
    }

    @Override
    public void sessionCreated(HttpSessionEvent evt) {
        HttpSession session = evt.getSession();
        if (session == null) {
            return;
        }

        log().info("Creating session " + StringUtils.defaultIfBlank(session.getId(), "{BLANK}"));
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent evt) {
        HttpSession session = evt.getSession();
        if (session == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Destroying session: ");
        sb.append(StringUtils.defaultIfBlank(session.getId(), "{BLANK}"));
        sb.append(" ");
        app(sb, session, Const.SESS_USR_EMAIL);
        app(sb, session, Const.SESS_USR_NAME);
        app(sb, session, Const.SESS_USR_LOC);
        app(sb, session, Const.SESS_USR_PHOTO);

        log().info(sb.toString());
    }
    private static void app(StringBuilder sb, HttpSession session, String name) {
        sb.append(name);
        sb.append("='");
        sb.append(StringUtils.defaultIfBlank((String)session.getAttribute(name), ""));
        sb.append("' ");
    }
}

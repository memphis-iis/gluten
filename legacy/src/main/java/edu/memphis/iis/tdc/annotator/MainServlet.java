package edu.memphis.iis.tdc.annotator;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.config.SimpleEncrypt;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.ModeSource;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;

/*
 * TODO: "real" training mode
 *   1. Will be set via mode source in assignment mode
 *   2. Our "virtual" check here will be unnecessary - remove all that stuff
 *   3. Change edit servlet to look at mode source instead of training file existence
 *   4. Update transcript session comments for isTraining
 */

/**
 * Our main servlet that handles initialization and the index/home stuff
 */
@WebServlet(value="/home", loadOnStartup=1)
public class MainServlet extends ServletBase {
    private static final long serialVersionUID = 1L;

    @Override
    public void init() throws ServletException {
        //First things first - get the config context.  In our tiny lightweight
        //framework, this forces the configuration init - before any requests.
        //If we were using something like Spring, this would already be done.
        ConfigContext configContext = ConfigContext.getInst();
        log().info(String.format("Configuration completed: Found %d dialog acts",
                configContext.getTaxonomy().getDialogActs().size()));

        //Probably the only place we'll specify the level for an audit, but
        //we want this one in our error log as well
        audit(Level.WARN, "Server init has completed");
    }

    @Override
    protected String doProtectedGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        //Note that if we're here, they are already logged in
        if (!StringUtils.isBlank(request.getParameter("do_logout"))) {
            request.getSession(true).invalidate();
            seeOther(request, response, buildAppURL(request, "loggedout.html"));
            return NO_VIEW;
        }

        ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();

        String userEmail = getUserEmail(request);

        //Get all the training filenames so that we can mark any transcripts
        //as training mode
        String trainer = ctx.getString(Const.PROP_TRAINER_NAME, "");
        Set<String> trainNames = tserv.findAllFiles(trainer, State.Completed, null).keySet();

        request.setAttribute(Const.REQ_SESS_PEND, findSessions(tserv, State.Pending, userEmail, trainNames));
        request.setAttribute(Const.REQ_SESS_INPROG, findSessions(tserv, State.InProgress, userEmail, trainNames));
        request.setAttribute(Const.REQ_SESS_COMP, findSessions(tserv, State.Completed, userEmail, trainNames));

        return "/WEB-INF/view/home.jsp";
    }

    @Override
    protected String doProtectedPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        throw new UserErrorException("Something is wrong - the HTTP action is supported for this page");
    }

    //Find a list of sessions in the given state for the current user - also
    //note that we handle any annotation needed by a view
    private List<TranscriptSession> findSessions(
            TranscriptService tserv,
            TranscriptService.State state,
            String userEmail,
            Set<String> trainNames)
    {
        List<TranscriptSession> ret = tserv.getUserTranscripts(state, userEmail);

        for(TranscriptSession ts: ret) {
            String baseFileName = ts.getBaseFileName();
            if (StringUtils.isNotBlank(baseFileName)) {
                ts.setWebFileName(SimpleEncrypt.hideString(baseFileName));
            }

            //If this transcript is in "training" mode, set a dummy
            if (trainNames.contains(baseFileName)) {
                ModeSource ms = new ModeSource();
                ms.setMode("training");
                TranscriptFileInfo tfi = new TranscriptFileInfo();
                tfi.setUser("trainer");
                tfi.setFileName(baseFileName);
                tfi.setState(State.Completed);
                ms.getSources().add(tfi);
                ts.setModeSource(ms);
            }
        }

        return ret;
    }
}

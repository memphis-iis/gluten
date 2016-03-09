package edu.memphis.iis.tdc.annotator;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import edu.memphis.iis.tdc.annotator.data.TranscriptService.TfiDateDescSort;
import edu.memphis.iis.tdc.annotator.model.ModeSource;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;
import edu.memphis.iis.tdc.annotator.model.Utterance;

/**
 * Servlet handling VERIFY admin function.
 *
 * <p>We show all transcripts eligible for verification AND
 * those transcripts already started for verification.  The user can start
 * a "verification" session on a transcript, which creates a special version
 * that "know" what source transcripts are in use and is somewhat "pre-tagged".
 * In essence, a diff-merge operation.
 * </p>
 */
@WebServlet(value="/admin-verify", loadOnStartup=3)
public class AdminVerifyServlet extends ServletBase {
    private static final long serialVersionUID = 1L;

    //helper sort for bean maps of tfi's
    private static final class TfiMapDateDescSort implements Comparator<Map<String, Object>>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            Date d1 = (Date)o1.get("maxLastModified");
            Date d2 = (Date)o2.get("maxLastModified");
            return d2.compareTo(d1);
        }
    };

    @Override
    protected String doProtectedGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        if (!(boolean)request.getAttribute(Const.REQ_IS_VERIFIER)) {
            throw new UserErrorException("Unauthorized Request Made");
        }

        ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();

        //Information that we'll need
        String userEmail = getUserEmail(request);
        String trainer = ctx.getString(Const.PROP_TRAINER_NAME);

        //First we need everything that is completed (since that is what
        //is eligible for verification)
        Map<String, List<TranscriptFileInfo>> possibles = tserv.findAllFiles(null, State.Completed, null);

        //Get everything for the current user (who is the verifier)
        Map<String, List<TranscriptFileInfo>> currUser = tserv.findAllFiles(userEmail, null, null);

        //Figure what is currently being verified (and what has already been verified)
        Set<String> notAvailable = new HashSet<String>();
        List<TranscriptFileInfo> currentVerifyList = new ArrayList<TranscriptFileInfo>();

        for(List<TranscriptFileInfo> infos: currUser.values()) {
            for(TranscriptFileInfo info: infos) {
                //If the current user is working with it, it's not available
                notAvailable.add(info.getFileName());
                if (info.getState() != State.Completed) {
                    currentVerifyList.add(info);
                }
            }
        }

        //Finish the "current verification list":
        //Set web file name for the current verify list (so there can be links).
        //The Hitch: the verifier user might be tagging something that is NOT in
        //verify mode.  To check this, we need to open each transcript and see
        //(Note our manual loop down since we might delete)
        Collections.sort(currentVerifyList, new TfiDateDescSort());
        for(int i = currentVerifyList.size() - 1; i >= 0; --i) {
            TranscriptFileInfo tfi = currentVerifyList.get(i);
            TranscriptSession ts = tserv.getSingleTranscript(tfi);

            if (!ts.isVerify()) {
                currentVerifyList.remove(i);
                continue;
            }

            tfi.setWebFileName(SimpleEncrypt.hideString(tfi.getFileName()));
        }

        //Now, if it's completed and available, we want to show it
        //Note that we are handling the model displayed by a template,
        //which should handle EITHER a map or statically-typed object.
        //We're going to construct this as a map so we don't create a
        //special type for a dynamically-typed template language
        List<Map<String, Object>> possibleVerifyList = new ArrayList<Map<String, Object>>();

        for (Map.Entry<String, List<TranscriptFileInfo>> entry: possibles.entrySet()) {
            String fileName = entry.getKey();
            if (notAvailable.contains(fileName))
                continue;

            List<TranscriptFileInfo> infos = entry.getValue();

            //We need at least one entry for a transcript - and at least one
            //must NOT be a training transcript
            if (infos == null || infos.size() < 1) {
                continue;
            }
            if (infos.size() == 1 && trainer.equals(infos.get(0).getUser())) {
                continue;
            }

            //Sort by date desc for display (and to figure out max date)
            Collections.sort(infos, new TfiDateDescSort());

            //Setup from the first item in the list, then add the other
            //items so that we can show the various sources that will be
            //used for verification (note that our sort above gives us
            Map<String, Object> item = Utils.objectToMap(infos.get(0));
            item.remove("absoluteFilePath");
            item.put("maxLastModified", infos.get(0).getLastModified());
            item.put("children", infos);
            possibleVerifyList.add(item);
        }

        //Now sort the entire list by date desc
        Collections.sort(possibleVerifyList, new TfiMapDateDescSort());

        request.setAttribute(Const.REQ_TOVERIFY_LIST, possibleVerifyList);
        request.setAttribute(Const.REQ_CURRVERIFY_LIST, currentVerifyList);

        return "/WEB-INF/view/verify.jsp";
    }

    @Override
    protected String doProtectedPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        if (!(boolean)request.getAttribute(Const.REQ_IS_VERIFIER)) {
            throw new UserErrorException("Unauthorized Request Made");
        }

        ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();

        //Information that we'll need
        String userEmail = getUserEmail(request);
        String trainer = ctx.getString(Const.PROP_TRAINER_NAME);

        String fileName = request.getParameter("filename");
        if (StringUtils.isBlank(fileName)) {
            throw new UserErrorException("No file name specified for verification");
        }

        List<TranscriptFileInfo> toVerify = tserv.findAllFiles(null, null, fileName).get(fileName);
        if (toVerify == null) {
            throw new UserErrorException("Could not find the file requested for verification");
        }

        //Iterate using indexes descending since we'll be deleting
        //We need to make sure that this isn't already being verified AND
        //we need to remove any instances that aren't ready for verification
        for(int i = toVerify.size() - 1; i >= 0; --i) {
            TranscriptFileInfo tfi = toVerify.get(0);
            if (userEmail.equals(tfi.getUser())) {
                throw new UserErrorException("Already assigned to verification user: " + fileName);
            }
            else if (tfi.getState() != State.Completed || trainer.equals(tfi.getUser())) {
                toVerify.remove(i);
            }
        }

        //Maybe there's nothing left?
        if (toVerify.size() < 1) {
            throw new UserErrorException("No copies of the file are ready for verification:  " + fileName);
        }

        //Go ahead and force an order on the verify list
        Collections.sort(toVerify, new TfiDateDescSort());

        //Need to read all the verification copies
        List<TranscriptSession> sessions = new ArrayList<TranscriptSession>(toVerify.size());
        for(TranscriptFileInfo tfi: toVerify) {
            TranscriptSession session = tserv.getSingleTranscript(tfi);
            if (session == null) {
                throw new UserErrorException("Transcript load for verify failed: : " + tfi.toString());
            }
            sessions.add(session);
        }

        //Create a new transcript - we use the first transcript for the model.
        //Note that this is also the first in the sorted order, so our default
        //tags will be from the MOST RECENT source
        TranscriptSession verifySession = sessions.get(0);

        //We need to mark this as a verification transcript and save it's references
        ModeSource modeSource = new ModeSource();
        modeSource.setMode("verify");
        modeSource.getSources().addAll(toVerify);
        verifySession.setModeSource(modeSource);

        //Verifications start out unsure about every tag, and they don't have
        //comments (remember that we'll be cramming all the user comments
        //together in one giant comment in the backing "training" script
        for(Utterance utt: verifySession.getTranscriptItems()) {
            utt.setTagConfidence(0);
            utt.setComments("");
        }

        //Now save the new transcript
        try {
            tserv.writeTranscript(verifySession, State.Pending, userEmail, fileName);
        }
        catch(IOException e) {
            userAudit(Level.WARN, "Could not write verify file", request, State.Pending, fileName, verifySession);
            throw new UserErrorException("Could not write verification file " + fileName, e);
        }

        userAudit("Transcript VERIFY-WRITE", request, State.Pending, fileName, verifySession);

        //Now redirect them to the verification that they just started
        String url = buildAppURL(request, "edit") + String.format("?state=%s&fn=%s",
                State.Pending,
                SimpleEncrypt.hideString(fileName));

        seeOther(request, response, url);
        return NO_VIEW;
    }
}

package edu.memphis.iis.tdc.annotator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.config.SimpleEncrypt;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;
import edu.memphis.iis.tdc.annotator.model.Utterance;

/**
 * Provide editing functionality for a transcript - obviously this is
 * our main annotating functionality
 */
@WebServlet(value="/edit", loadOnStartup=3)
public class EditServlet extends ServletBase {
    private static final long serialVersionUID = 1L;

    @Override
    protected String doProtectedGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();

        TranscriptService.State state = parseState(request.getParameter("state"));
        String baseFileName = getTargetFilename(request.getParameter("fn"));

        String userEmail = getUserEmail(request);

        //We now know that we have valid parameters (at least until we try to read)
        TranscriptSession ts = tserv.getSingleTranscript(state, userEmail, baseFileName);

        if (ts == null) {
            throw new UserErrorException("Could not find the request transcript");
        }

        //If we successfully opened a pending transcript, then we should
        //move it to InProgress and redirect them
        if (state == State.Pending) {
            try {
                tserv.moveTranscript(state, State.InProgress, userEmail, baseFileName);
            }
            catch(IOException e) {
                throw new UserErrorException("Could not move the file from Pending to InProgress", e);
            }

            userAudit("Transcript MOVED from Pending to InProgress", request, null, baseFileName, ts);

            String url = String.format("%s?state=%s&fn=%s",
                request.getRequestURL(),
                TranscriptService.State.InProgress,
                SimpleEncrypt.hideString(baseFileName));

            seeOther(request, response, url);
            return NO_VIEW;
        }


        //Log the open now that we know we won't redirect
        userAudit("Transcript OPEN", request, state, baseFileName, ts);

        TranscriptSession trainScript = null;
        boolean inVerify = ts.isVerify();
        boolean inTraining = false; //will be set below
        boolean inAssess = false;   //only set by completed check below
        String reportedMode = "";   //for audit log

        //Is this a verify script? Then we need some extra setup
        if (inVerify) {
            //Verify - the trainer script needs to be built
            reportedMode = "VERIFY";
            trainScript = buildVerifyTrainer(tserv, ts);
            if (trainScript == null) {
                throw new UserErrorException("Could not find any sources to use for verifcation with " + ts.getBaseFileName());
            }
        }
        else {
            //Not verify - might be in training mode
            //Is there a trainer transcript available?
            //Note that any kind of mode (like verification) skips trainer mode
            String trainer = ctx.getString(Const.PROP_TRAINER_NAME, "");
            if (StringUtils.isNotBlank(trainer) && !ts.isInMode()) {
                trainScript = tserv.getSingleTranscript(State.Training, userEmail, baseFileName);
                if(trainScript == null)
                    trainScript = tserv.getSingleTranscript(State.Completed, trainer, baseFileName);
                //If we found a training transcript, we're in training mode
                inTraining = trainScript != null;
                if (inTraining)
                    reportedMode = "TRAINING";
            }
        }

        //Special case override (note that we accept the trainScript selected above
        if(state == State.Completed ){
            inVerify = false;
            inTraining = false;
            inAssess = true;
            reportedMode = "COMPLETED (FORCED ASSESS MODE)";
        }

        //Get the list of files for the prev/next buttons and then figure out the
        //prev/next button content (if there is any)
        List<State> fileStates = new ArrayList<State>();
        if (state == State.Pending || state == State.InProgress) {
            fileStates.add(State.Pending);
            fileStates.add(State.InProgress);
        }
        else {
            fileStates.add(state);
        }
        List<TranscriptFileInfo> fileList = findFileList(tserv, userEmail, fileStates);

        TranscriptFileInfo prevFile = null;
        TranscriptFileInfo nextFile = null;

        for(int i = 0; i < fileList.size(); ++i) {
            TranscriptFileInfo curr = fileList.get(i);
            if (curr.getState().equals(state) &&
                curr.getFileName().equalsIgnoreCase(baseFileName) &&
                curr.getUser().equalsIgnoreCase(userEmail))
            {
                if (i > 0) {
                    prevFile = fileList.get(i-1);
                }
                if (i < fileList.size() - 1) {
                    nextFile = fileList.get(i+1);
                }
                break;
            }
        }

        //If we found any files, we need to set up their "web" file names
        if (prevFile != null) {
            prevFile.setWebFileName(SimpleEncrypt.hideString(prevFile.getFileName()));
        }
        if (nextFile != null) {
            nextFile.setWebFileName(SimpleEncrypt.hideString(nextFile.getFileName()));
        }

        //Set mode options (and possibly the training transcript).
        request.setAttribute("verifyMode", inVerify);
        request.setAttribute("trainingMode", inTraining);
        request.setAttribute("assessMode", inAssess);
        request.setAttribute("trainerTranscript", trainScript);
        request.setAttribute("prevFile", prevFile);
        request.setAttribute("nextFile", nextFile);

        //Audit a "special" mode transcript
        if (!StringUtils.isBlank(reportedMode)) {
            userAudit("Sending " + reportedMode + " Transcript", request, state, baseFileName, ts);
        }

        //Render the tagging screen for them
        request.setAttribute("transcript", ts);
        return "/WEB-INF/view/edit.jsp";
    }

    @Override
    protected String doProtectedPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();

        TranscriptService.State state = parseState(request.getParameter("state"));
        String baseFileName = getTargetFilename(request.getParameter("fn"));

        //We now know that we have valid parameters (at least until we try to read)

        String userEmail = getUserEmail(request);

        //Read what we actually have on disk
        TranscriptSession ts = tserv.getSingleTranscript(state, userEmail, baseFileName);
        if (ts == null) {
            throw new UserErrorException("Could not find the request transcript");
        }

        //Read and set any top-level information
        ts.setSoundness(getStrParm(request, "soundness"));
        ts.setSessionComments(getStrParm(request, "sessionComments"));
        ts.setLearningAssessmentScore(getStrParm(request, "learningAssessmentScore"));
        ts.setLearningAssessmentComments(getStrParm(request, "learningAssessmentComments"));

        //Read the user's save request
        String autosaveParm = request.getParameter("autosave");
        String completeBtn = request.getParameter("completed");
        String rawData = request.getParameter("fulldata");
        boolean isAutoSave = false;

        //check if completed
        boolean completed=false;
        if (!StringUtils.isBlank(completeBtn)) {
            //set completed
            completed = true;
            userAudit("Transcript COMPLETE-BTN Accepted", request, state, baseFileName, ts);
        }

        //Check to see if this is an auto save request
        if (!StringUtils.isBlank(autosaveParm) && !StringUtils.isBlank(rawData)) {
            //We're goint to be autosaving
            isAutoSave = true;
            userAudit("Transcript AUTO-SAVE Accepted", request, state, baseFileName, ts);
        }
        else if (StringUtils.isBlank(rawData)) {
            //Not auto save, but no REAL data
            userAudit(Level.WARN, "Transcript SAVE-SKIPPED - no data supplied",
                    request, state, baseFileName, ts);
            seeOther(request, response, getFullURL(request));
            return NO_VIEW;
        }

        //For save, work thru the items and update each transcript item
        JSONArray fullData = new JSONArray(rawData);
        String autoSaveResponse = "";

        for(int i = 0; i < fullData.length(); ++i) {
            JSONObject item = fullData.getJSONObject(i);
            String act = item.getString("act");
            String subact = item.getString("subact");
            String mode = item.getString("mode");
            String comments = item.getString("comments");
            int confidence = Utils.safeParseInt(item.getString("confidence"), 1);
            int index = item.getInt("index");

            Utterance utt = ts.getTranscriptItems().get(index);
            utt.setDialogAct(act);
            utt.setDialogSubAct(subact);
            utt.setDialogMode(mode);
            utt.setComments(comments);
            utt.setTagConfidence(confidence);
        }

        //Actual write to disk
        try {
            java.util.Date date= new java.util.Date();
            String savedtime =  ""+(new  Timestamp(date.getTime()));
            ts.setLastSavedTime(savedtime);

            if(ctx.userIsVerifier(getUserEmail(request))){
                ts.setVerifier(userEmail);
            }
            if(ctx.userIsTagger(getUserEmail(request))){
                ts.setTagger(userEmail);
            }

            tserv.writeTranscript(ts, state, userEmail, baseFileName);
        }
        catch(IOException e) {
            userAudit(Level.WARN, "Could not write file", request, state, baseFileName, ts);
            if (isAutoSave) {
                autoSaveResponse = "Could not write file: " + e.getMessage();
            }
            else {
                throw new UserErrorException("Could not write file " + baseFileName, e);
            }
        }

        //Now we write out audit
        userAudit("Transcript SAVE", request, state, baseFileName, ts);

        /* TODO: the "autosave, completed, or vanilla redirect" block below has
         * gotten out of control... refactor this code (or this method) to be
         * much leaner and cleaner
         * */

        //If this is an autosave, we just return a status JSON
        //If tagging is complete (and the transcript is already in completed)
        //then we should move it to completed. Otherwise they should be able
        //to keep tagging
        if (isAutoSave) {
            //Auto save - they get a JSON result
            JSONObject autoResult = new JSONObject();
            autoResult.put("success", StringUtils.isBlank(autoSaveResponse));
            autoResult.put("errmsg", autoSaveResponse);
            response.setContentType("application/json");
            response.getWriter().write(autoResult.toString());
        }
        else if (state != State.Completed  && completed) {
            //Tagging complete - move to completed and go back home

            try {
                java.util.Date date= new java.util.Date();
                String ttime =  ""+(new  Timestamp(date.getTime()));
                if(ctx.userIsVerifier(getUserEmail(request))){
                    //get the list of verifiers from context look at userInProp in ConfigContext
                    ts.setVerifiedTime(ttime);
                    tserv.writeTranscript(ts, State.Training, ts.getTagger(), ts.getBaseFileName());
                    tserv.writeTranscript(ts, State.Completed, "MASTER", ts.getBaseFileName());
                    //tserv.writeTranscript(ts, State.Pending, ts.getTagger(), ts.getBaseFileName());

                }else{
                    //now we also check for the Tagger role
                    if(ctx.userIsTagger(getUserEmail(request))){
                    File[] userdirs = tserv.findAllUserDirs();
                    ArrayList<String> verif = new ArrayList<String>();// "NA";
                    int min = 9999;
                    boolean duplicate = false;
                    for (int i = 0; i < userdirs.length; i++) {
                        String tempname = userdirs[i].getName();
                        if(ctx.userIsVerifier(tempname)){
                            Map<String, List<TranscriptFileInfo>> templist = tserv.findAllFiles(tempname, null, baseFileName);
                            if(templist.size()>0){
                                duplicate = true;
                            }
                            templist = tserv.findAllFiles(tempname, State.Pending, null);
                            if(templist.size()<min){
                                verif.clear();
                                verif.add(tempname);
                                min = templist.size();
                            }
                            if(templist.size()==min){
                                verif.add(tempname);
                                //min = templist.size();
                            }
                        }
                    }
                    String verifname = "NA";
                    if(verif.size()>0){
                        Random rand = new Random();
                        verifname = verif.get(rand.nextInt(verif.size()));
                    }
                    Map<String, List<TranscriptFileInfo>> templist = tserv.findAllFiles("untagged", State.Pending, null);

                    if(templist.size()>0){
                        for (Map.Entry<String, List<TranscriptFileInfo>> entry : templist.entrySet()) {
                            List<TranscriptFileInfo> tfl = entry.getValue();
                            TranscriptSession nextTr = tserv.getSingleTranscript(tfl.get(0));
                            tserv.writeTranscript(nextTr, State.Pending, userEmail, nextTr.getBaseFileName());
                            tserv.moveTranscript(State.Pending, State.Completed, "untagged", nextTr.getBaseFileName());
                            break;
                        }
                    }
                    ts.setTaggedTime(ttime);
                    if(!duplicate){
                        ts.setVerifier(verifname);
                        tserv.writeTranscript(ts, State.Pending, verifname, ts.getBaseFileName());
                    }
                }}
                //tserv.writeTranscript(ts, state, userEmail, baseFileName);
                tserv.moveTranscript(state, State.Completed, userEmail, baseFileName);
            }
            catch(IOException e) {
                throw new UserErrorException("Could not move the file to Completed", e);
            }

            userAudit("Transcript MOVED to Completed", request, null, baseFileName, ts);
            seeOther(request, response, "home");
        }
        else {
            //Otherwise, we just let them keep tagging
            seeOther(request, response, getFullURL(request));
        }

        //This post always fires a redirect
        return NO_VIEW;
    }

    //Given a "hidden" version of the file name, unhide it, validate it, and return it
    private String getTargetFilename(String hidden) throws UserErrorException {
        if (StringUtils.isBlank(hidden)) {
            throw new UserErrorException("No target filename was specified");
        }
        try {
            String s = SimpleEncrypt.unhideString(hidden); //Might throw unchecked exception
            if (StringUtils.isBlank(s)) {
                throw new Exception("No filename was specified");
            }
            return s;
        }
        catch(Throwable t) {
            throw new UserErrorException("Could not determine the file matching " + hidden, t);
        }
    }

    //Simple helper for some repeated logic in this file
    private String getStrParm(HttpServletRequest request, String name) {
    	String val = request.getParameter(name);
    	if (StringUtils.isBlank(val))
    		val = "";
    	return val;
    }

    //Given a transcript mode source instance, build the backing "trainer" transcript to match
    private TranscriptSession buildVerifyTrainer(TranscriptService tserv, TranscriptSession ts)
            throws UserErrorException
    {
        if (!ts.isVerify()) {
            return null; //Not a verify mode script!
        }

        List<TranscriptFileInfo> sources = ts.getModeSource().getSources();
        if (sources.size() <= 1) {
            //No alternative sources - just use the transcript itself as
            //the backing script
            return ts;
        }

        //From here on our we know that we have at least 2 transcripts

        //Get the reference transcripts we be reading
        List<TranscriptSession> refs = new ArrayList<TranscriptSession>();
        for(TranscriptFileInfo tfi: sources) {
            TranscriptSession one = tserv.getSingleTranscript(tfi);
            if (one != null)
                refs.add(one);
        }

        //Check that our utterance lists are the same size (this generally
        //shouldn't happen)
        int uttSize = ts.getTranscriptItems().size();
        for (TranscriptSession s: refs) {
            if (s.getTranscriptItems().size() != uttSize) {
                //This shouldn't happen - these transcripts don't line up
                throw new UserErrorException("Mismatched transcripts found " +
                        "in verification sources for " + ts.getBaseFileName());
            }
        }

        //If some of our references are no longer there, we need to supply
        //SOMETHING for the backing script
        if (refs.size() != sources.size()) {
            audit(Level.WARN, "Verify issue - at least one reference transcript was NOT found");
            if (refs.size() < 1) {
                return ts;
            }
            else if (refs.size() == 1) {
                return refs.get(0);
            }
        }

        //Remember that the original source was index 0 and the "next most
        //recent" is index 1.  Also note that if referenced transcripts have
        //disappeared, this might not be valid, but if we're here we still
        //have at least 2 transcripts...
        TranscriptSession trainer = refs.get(1);

        //Build comments
        for(int i = 0; i < uttSize; ++i) {
            Utterance firstUtt = ts.getTranscriptItems().get(i);
            String firstTag = firstUtt.getTagRepr(); //Simple way to check tag matches

            Utterance target = trainer.getTranscriptItems().get(i);

            StringBuilder comment = new StringBuilder();

            for(TranscriptSession ref: refs) {
                Utterance utt = ref.getTranscriptItems().get(i);

                //If this isn't the "REAL" utterance, there isn't a comment,
                //then we need to make sure there's a comment if and the
                //tagging is different
                if (utt != firstUtt && StringUtils.isBlank(utt.getComments())) {
                    if (!utt.getTagRepr().equals(firstTag)) {
                        utt.setComments("[Annotator]: Note the different tag");
                    }
                }

                if (StringUtils.isNotBlank(utt.getComments())) {
                    comment.append(utt.getDialogAct());
                    comment.append(":");
                    comment.append(utt.getDialogSubAct());
                    comment.append("=>");
                    comment.append(utt.getComments());
                    comment.append("{end}"); //UI should probably turn this into a line break
                }
            }

            target.setComments(comment.toString());
        }

        //Note: here is where we would skip ref 0 (since it's the base) and
        //check for tags different from refs.get(1)... but we're not currently
        //doing that.  (so just the 2 most recent tags for verification, but
        //all comments)

        return trainer;
    }

    //Return a sorted list of file info instances for the given user
    //in the given states
    private List<TranscriptFileInfo> findFileList(TranscriptService tserv, String user, List<State> states) {
        List<TranscriptFileInfo> files = new ArrayList<TranscriptFileInfo>();

        //Just grab all the files - note that we don't care about duplicates
        for(State state: states) {
            Map<String, List<TranscriptFileInfo>> found = tserv.findAllFiles(user, state, null);
            for(List<TranscriptFileInfo> oneList: found.values()) {
                files.addAll(oneList);
            }
        }

        //Now sort
        Collections.sort(files, new Comparator<TranscriptFileInfo>(){
            @Override
            public int compare(TranscriptFileInfo lhs, TranscriptFileInfo rhs) {
                return new CompareToBuilder()
                    .append(lhs.getFileName(), rhs.getFileName(), String.CASE_INSENSITIVE_ORDER)
                    .append(lhs.getState().ordinal(), rhs.getState().ordinal())
                    .toComparison();
            }
        });

        return files;
    }
}

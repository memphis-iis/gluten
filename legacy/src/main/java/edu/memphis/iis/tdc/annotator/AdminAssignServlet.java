package edu.memphis.iis.tdc.annotator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.json.JSONObject;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;

/**
 * Servlet handling ASSIGN admin function.
 *
 * <p>We show untagged transcripts and available users so that the assigner can
 * give them a "pending" transcript.  Think of this as the "file copy" module
 * </p>
 */
@WebServlet(value="/admin-assign", loadOnStartup=3)
public class AdminAssignServlet extends ServletBase {
    private static final long serialVersionUID = 1L;

    private static final class TfiAssignViewSort implements Comparator<TranscriptFileInfo>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override public int compare(TranscriptFileInfo o1, TranscriptFileInfo o2) {
            return new CompareToBuilder()
                .append(o1.getUser(), o2.getUser())
                .append(o1.getState().toString(), o2.getState().toString())
                .append(o1.getFileName(), o2.getFileName())
                .append(o2.getLastModified(), o2.getLastModified())
                .toComparison();
        }
    };

    @Override
    protected String doProtectedGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        if (!(boolean)request.getAttribute(Const.REQ_IS_ASSIGNER)) {
            throw new UserErrorException("Unauthorized Request Made");
        }

        ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();

        //Find everything we'll let them look at
        Map<String, List<TranscriptFileInfo>> possibles = tserv.findAllFiles(null, null, null);

        //Now build what they will be able to see
        List<TranscriptFileInfo> allFiles = new ArrayList<TranscriptFileInfo>();
        for(Map.Entry<String, List<TranscriptFileInfo>> entry: possibles.entrySet()) {
            for(TranscriptFileInfo tfi: entry.getValue()) {
                allFiles.add(tfi);
            }
        }

        //And handle the initial sort order
        Collections.sort(allFiles, new TfiAssignViewSort());

        //Easy: need lists of States and Users
        String[] states = new String[] {
            State.Pending.toString(),
            State.InProgress.toString(),
            State.Completed.toString(),
            State.Training.toString()
        };

        List<String> userDirs = new ArrayList<String>();
        for(File f: tserv.findAllUserDirs()) {
            userDirs.add(f.getName());
        }

        //Ta-da
        request.setAttribute(Const.REQ_TOASSIGN_LIST, allFiles);
        request.setAttribute(Const.REQ_ASSIGN_STATES, states);
        request.setAttribute(Const.REQ_ASSIGN_USERS, userDirs);

        return "/WEB-INF/view/assign.jsp";
    }

    @Override
    protected String doProtectedPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        if (!(boolean)request.getAttribute(Const.REQ_IS_ASSIGNER)) {
            throw new UserErrorException("Unauthorized Request Made");
        }

        String errMsg = "";
        ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();

        try {
            //Information that we'll need
            String fileName = request.getParameter("filename");
            if (StringUtils.isBlank(fileName)) {
                throw new UserErrorException("No file name specified for assignment");
            }
            String fileUser = request.getParameter("user");
            if (StringUtils.isBlank(fileUser)) {
                throw new UserErrorException("No user specified for assignment");
            }
            State fileState = parseState(request.getParameter("state"));

            TranscriptSession ts = tserv.getSingleTranscript(fileState, fileUser, fileName);
            if (ts == null) {
                throw new UserErrorException("The selected file could not be found");
            }
            userAudit("ASSIGN READ Transcript", request, fileState, fileName, ts);

            String targetUser = request.getParameter("targetuser");
            if (StringUtils.isBlank(targetUser)) {
                throw new UserErrorException("No target user specified for assignment");
            }
            State targetState = parseState(request.getParameter("targetstate"));

            tserv.writeTranscript(ts, targetState, targetUser, fileName);
            userAudit("ASSIGN WRITE Transcript to " + targetUser, request, targetState, fileName, ts);
        }
        catch(Throwable t) {
            errMsg = t.getMessage();
            log().error("There was an error attempting to assign", t);
        }

        //Success - return what happened
        JSONObject assignResult = new JSONObject();
        assignResult.put("success", StringUtils.isBlank(errMsg));
        assignResult.put("errmsg", errMsg);
        response.setContentType("application/json");
        response.getWriter().write(assignResult.toString());
        return NO_VIEW;
    }
}

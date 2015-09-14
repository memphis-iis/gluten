package edu.memphis.iis.tdc.annotator;

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

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;

/**
 * Servlet handling ASSESS admin function.
 * 
 * <p>We allow someone to pick 2 transcripts and view them in a manner similar
 * to training mode (a "main" transcript and a "backing" transcript)
 * </p>
 */
@WebServlet(value="/admin-assess", loadOnStartup=3)
public class AdminAssessServlet extends ServletBase {
    private static final long serialVersionUID = 1L;
    
    private static final class TfiAssessViewSort implements Comparator<TranscriptFileInfo>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override public int compare(TranscriptFileInfo o1, TranscriptFileInfo o2) {
            return new CompareToBuilder()
                .append(o1.getFileName(), o2.getFileName())    
                .append(o2.getLastModified(), o2.getLastModified())
                .append(o1.getState().toString(), o2.getState().toString())
                .append(o1.getUser(), o2.getUser())                
                .toComparison();
        }
    };
    
    @Override
    protected String doProtectedGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException, UserErrorException
    {
//        if (!(boolean)request.getAttribute(Const.REQ_IS_ASSESSOR)) {
//            throw new UserErrorException("Unauthorized Request Made");
//        }
        
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
        Collections.sort(allFiles, new TfiAssessViewSort());
        
        request.setAttribute(Const.REQ_TOASSESS_LIST, allFiles);
        
        return "/WEB-INF/view/assess.jsp";
    }

    //YES, this post will result in showing an edit page, which seems to
    //violate the PRG principle (which we use when saving an edit page).
    //HOWEVER, the edit page has no save and is ACTUALLY an admin mode
    //transient state (vs a verify action where we create something "real"
    //to be edited).
    @Override
    protected String doProtectedPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException 
    {
    	if (!(boolean)request.getAttribute(Const.REQ_IS_ASSESSOR)) {
            throw new UserErrorException("Unauthorized Request Made");
        }
    	
    	ConfigContext ctx = ConfigContext.getInst();
        TranscriptService tserv = ctx.getTranscriptService();
        
        //Information that we'll need        
        String fileName = request.getParameter("filename");
        if (StringUtils.isBlank(fileName)) {
            throw new UserErrorException("No file name specified for assessment");
        }
        String fileUser = request.getParameter("user");
        if (StringUtils.isBlank(fileUser)) {
            throw new UserErrorException("No user specified for assessment");
        }
        State fileState = parseState(request.getParameter("state"));
        
        TranscriptSession ts = tserv.getSingleTranscript(fileState, fileUser, fileName);
        if (ts == null) {
        	throw new UserErrorException("The selected file could not be found"); 
        }
        
        //These are optional
        String backUser = request.getParameter("trainuser");
        State backState = null;
        if (StringUtils.isNotBlank(backUser)) {
        	backState = parseState(request.getParameter("trainstate"));
        }
        
        TranscriptSession backScript = null;
        if (StringUtils.isNotBlank(backUser)) {
        	backScript = tserv.getSingleTranscript(backState, backUser, fileName);
        	if (backScript == null) {
        		throw new UserErrorException("The selected backing file could not be found");
        	}
        } 
        
        request.setAttribute("verifyMode", false);
        request.setAttribute("trainingMode", false);
        request.setAttribute("assessMode", true);
        request.setAttribute("trainerTranscript", backScript);
        
        userAudit("Sending ASSESS Transcript", request, fileState, fileName, ts);
        
        //Render the tagging screen for them
        request.setAttribute("transcript", ts);
        return "/WEB-INF/view/edit.jsp";
    }
}

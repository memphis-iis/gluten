package edu.memphis.iis.tdc.annotator.model;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;

/**
 * Information we know about a transcript file.  This is really only useful
 * for reporting the results of a file search that is too large for parsing.
 * Otherwise you could just use a list of TranscriptSessions.
 *
 * <p>Note that to read/write a transcript session file, you need the
 * user/owner, the state, and the file name.  Thus a properly populated
 * instance of this class has enough info to find and read the corresponding
 * transcript file</p>
 *
 * <p>Also note that webFileName is set by an external caller, and is
 * <em>never</em> set by a constructor</p>
 */
@Root(name="transcriptFileInfo")
public class TranscriptFileInfo {
    @Element private String fileName;
    private String webFileName;
    private String absoluteFilePath;
    private Date lastModified;
    @Element private State state;
    @Element private String user;

    /**
     * The recommended way to create a TranscriptFileInfo instance
     */
    public TranscriptFileInfo(File file, State state, String user) {
        this.absoluteFilePath = file.getAbsolutePath();
        this.fileName = FilenameUtils.getName(this.absoluteFilePath);
        this.webFileName = null;
        this.lastModified = new Date(file.lastModified());
        this.state = state;
        this.user = user;
    }

    /**
     * Allow updating via a File instance (useful for instances deserialized
     * from XML).  Note that true is only returned if the File instance matches
     * the current info in the class and everything is valid.
     */
    public boolean updateFromFile(File f) {
        //Need a valid file that matches the previously specified file name,
        //state, and user
        if (f == null || !f.isFile() || !f.exists()) {
            return false;
        }

        String absPath = f.getAbsolutePath();
        if (StringUtils.isBlank(absPath)) {
            return false;
        }

        if (StringUtils.isBlank(fileName) || !fileName.equals(FilenameUtils.getName(absPath))) {
            return false;
        }

        if (state == null || StringUtils.isBlank(user)) {
            return false;
        }
        if (!absPath.contains(state.toString()) || !absPath.contains(user)) {
            return false;
        }

        //OK - now we can set our data
        absoluteFilePath = absPath;
        lastModified = new Date(f.lastModified());
        return true;
    }

    /**
     * Default ctor's can be handy, but you're on your own for correct init
     */
    public TranscriptFileInfo() {
    }

    /**
     * File name for the transcript.  No path info is included
     */
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * The file name used for web links.  Note that it is never set in a
     * ctor and so must be set by an external caller when required
     */
    public String getWebFileName() {
        return webFileName;
    }
    public void setWebFileName(String webFileName) {
        this.webFileName = webFileName;
    }

    /**
     * The entire path to the file
     */
    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }
    public void setAbsoluteFilePath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }

    /**
     * Date that the file was last modified
     */
    public Date getLastModified() {
        return lastModified == null ? null : new Date(lastModified.getTime());
    }
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified == null ? null : new Date(lastModified.getTime());
    }

    /**
     * Current state of the transcript
     */
    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }

    /**
     * User that "owns" this file
     */
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * A simple helper override on toString for debugging.  The format isn't
     * guaranteed in any way
     */
    @Override
    public String toString() {
        return String.format("%s:%s:[%s]", user, state.toString(), fileName);
    }
}

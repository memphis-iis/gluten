package edu.memphis.iis.tdc.annotator.data;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.log4j.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.CamelCaseStyle;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.Style;

import edu.memphis.iis.tdc.annotator.Utils;
import edu.memphis.iis.tdc.annotator.model.ModeSource;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;

/**
 * Service interface for accessing transcripts
 */
public class TranscriptService {
    private final static Logger logger = Logger.getLogger(TranscriptService.class);

    private String userDirectory;

    public String TRANSCRIPT_EXTENSION = "xml";

    /**
     * Represents the current tagging state for a transcript
     */
    public enum State {
        Pending,
        InProgress,
        Completed,
        Training
    };

    //Custom sort for transcripts (by session ID)
    private final static class SessionIdSort implements Comparator<TranscriptSession>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public int compare(TranscriptSession lhs, TranscriptSession rhs) {
            return Long.compare(lhs.getScriptId(), rhs.getScriptId());
        }
    };

    /**
     * Utility sort used for ModeSource file info objects.  PLEASE Note
     * our lack of tolerance for null objects.
     */
    public static final class TfiDateDescSort implements Comparator<TranscriptFileInfo>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override public int compare(TranscriptFileInfo o1, TranscriptFileInfo o2) {
            return new CompareToBuilder()
                .append(o2.getLastModified(), o2.getLastModified())
                .append(o1.getUser(), o2.getUser())
                .append(o1.getState().toString(), o2.getState().toString())
                .append(o1.getFileName(), o2.getFileName())
                .toComparison();
        }
    };

    /**
     * Given a transcript tag state and a user name, return all
     * the transcripts found
     */
    public List<TranscriptSession> getUserTranscripts(State state, String userName) {
        return readTranscripts(findDir(state, userName));
    }

    /**
     * Given a transcript tag state, a user name, and the file name,
     * return the transcript found (or null if it's not found)
     */
    public TranscriptSession getSingleTranscript(State state, String userName, String fileName) {
        List<TranscriptSession> found = readTranscripts(findDir(state, userName), fileName);
        if (found.size() < 1)
            return null;

//        found.get(0).setTagger(userName);
//        found.get(0).setVerifier("VV"+userName);
        return found.get(0);

    }

    /**
     * Simple overloaded wrapper that allows easy loading of a transcript
     * from an info reference (from findAllFiles)
     */
    public TranscriptSession getSingleTranscript(TranscriptFileInfo info) {
        if (info == null)
            return null;
        return getSingleTranscript(
            info.getState(),
            info.getUser(),
            info.getFileName());
    }

    /**
     * Find all files in a given state.  The structure returned is a map from a file
     * name to a list of the instances of that file found (per user and state).  Note
     * that any single instance in the (value) list can be used to read the actual
     * transcript file.
     * @param userName user to search for.  If null, then all users will be searched
     * @param state state to search for.  If null, then all states will be searched
     * @param filename filename to search for.  If null, then all files will be searched
     * @return map of files found (see above)
     */
    public Map<String, List<TranscriptFileInfo>> findAllFiles(String userName, State state, String filename) {
        Map<String, List<TranscriptFileInfo>> files;
        files = new TreeMap<String, List<TranscriptFileInfo>>();

        //What users do we search?
        File[] userDirs;
        if (StringUtils.isBlank(userName)) {
            userDirs = findAllUserDirs();
        }
        else {
            userDirs = new File[] { new File(userDirectory, userName) };
        }

        //What states do we search?
        List<State> stateSearch = new ArrayList<State>();
        if (state != null) {
            stateSearch.add(state);
        }
        else {
            stateSearch.add(State.Pending);
            stateSearch.add(State.InProgress);
            stateSearch.add(State.Completed);
        }

        //Treat non-null-but-blank filenames as null
        if (StringUtils.isBlank(filename))
            filename = null;

        for(File userDir: userDirs) {
            String currUser = FilenameUtils.getName(userDir.getAbsolutePath());
            for (State s: stateSearch) {
                for (File transcript: findTranscripts(findDir(s, currUser), filename)) {
                    String fn = FilenameUtils.getName(transcript.getAbsolutePath());
                    List<TranscriptFileInfo> vals = files.get(fn);
                    if (vals == null) {
                        vals = new ArrayList<TranscriptFileInfo>();
                        files.put(fn, vals);
                    }
                    vals.add(new TranscriptFileInfo(transcript, s, currUser));
                }
            }
        }

        return files;
    }

    /**
     * Return an array of File instances representing all user directories.
     * @return
     */
    public File[] findAllUserDirs() {
        File baseDir = new File(userDirectory);
        return baseDir.listFiles((FileFilter)FileFilterUtils.directoryFileFilter());
    }

    /**
     * Move the specified file from one state to another.
     * @throws IOException
     */
    public void moveTranscript(State from, State to, String userName, String fileName) throws IOException {
        if (from.equals(to)) {
            logger.warn("Move request ignored: from=to, so no-op");
            return;
        }

        TranscriptSession src = getSingleTranscript(from, userName, fileName);

        TranscriptSession dest = getSingleTranscript(to, userName, fileName);

        boolean deleteDest = false;

        if (src == null) {
            if (dest == null) {
                String errMsg = String.format(
                    "Requested file for move doesn't exist [user:%s filename:%s]",
                    userName,
                    fileName);
                logger.error(errMsg);
                throw new IOException(errMsg);
            }
            else { //dest != null
                logger.warn(
                    "Move request silently ignored because src was " +
                    "missing and dest file was already there: " +
                    dest.getSourceFileName());
                return;
            }
        }
        else { //src != null
            if (dest != null) {
                String warnMsg = String.format(
                    "Requested file for move exists in BOTH places " +
                    "[user:%s filename:%s] - will attempt to delete destination",
                    userName,
                    fileName);
                logger.warn(warnMsg);
                deleteDest = true;
            }
        }

        //src is not null AND dest is null

        File srcFile = new File(src.getSourceFileName());
        File destFile = new File(findDir(to, userName), fileName);
        if (deleteDest) {
            FileUtils.deleteQuietly(destFile);
        }
        FileUtils.moveFile(srcFile, destFile);
    }

    /**
     * Write out the given transcript
     * @throws IOException
     */
    public void writeTranscript(
            TranscriptSession session,
            State state,
            String userName,
            String fileName)
            throws IOException
    {
//    	session.setTagger(userName);
//    	session.setVerifier("VvV");
        File targetFile = new File(findDir(state, userName), fileName);
        Serializer serializer = createSerializer();
        try {
            writeOne(serializer, targetFile, session);
        }
        catch (Exception e) {
            throw new IOException("Could write file " + targetFile.getAbsolutePath(), e);
        }
    }

    /**
     * Data storage location where transcripts are stored.  Should be set
     * before we are used (which should be handled by ConfigContext unless
     * you're testing)
     */
    public String getUserDirectory() {
        return userDirectory;
    }

    public void setUserDirectory(String userDirectory) {
        this.userDirectory = userDirectory;
    }

    protected File findDir(State state, String userName) {
        String dirName = userDirectory + "/" + userName + "/" + state.toString();
        return new File(Utils.checkDir(dirName));
    }

    protected List<TranscriptSession> readTranscripts(File dir) {
        return readTranscripts(dir, null);
    }

    //Note that currently filePattern must completely match the file name
    protected List<TranscriptSession> readTranscripts(File dir, String filePattern) {
        List<TranscriptSession> sessions = new ArrayList<TranscriptSession>();

        Serializer serializer = createSerializer();
        for(File f: findTranscripts(dir, filePattern)) {
            try {
                sessions.add(parseOne(serializer, f));
            }
            catch (Exception e) {
                logger.warn("Skipped unreadable file: " + f.getAbsolutePath(), e);
            }
        }

        //Note our null check above so we assume no null compares in our sort
        Collections.sort(sessions, new SessionIdSort());

        //Now sort each sessions mode source (if there is one)

        for(TranscriptSession session: sessions) {
            ModeSource ms = session.getModeSource();
            if (ms != null && ms.getSources().size() > 0) {
                Collections.sort(ms.getSources(), new TfiDateDescSort());
            }
        }

        return sessions;
    }

    protected Collection<File> findTranscripts(File dir, String filePattern) {
        IOFileFilter fileFilter = FileFilterUtils.suffixFileFilter(TRANSCRIPT_EXTENSION);

        if (StringUtils.isNotBlank(filePattern)) {
            fileFilter = FileFilterUtils.and(
                    fileFilter,
                    FileFilterUtils.nameFileFilter(filePattern));
        }

        //Get all XML files, no recursion
        return FileUtils.listFiles(dir,  fileFilter, null);
    }

    protected Serializer createSerializer() {
        Style style = new CamelCaseStyle(true);
        Format format = new Format(style);
        return new Persister(format);
    }

    protected TranscriptSession parseOne(Serializer serializer, File f) throws Exception {
        TranscriptSession ts = serializer.read(TranscriptSession.class, f);
        if (ts == null) {
            throw new Exception("Null session deserialized - this shouldn't happen?");
        }

        //Remember the file name
        ts.setSourceFileName(f.getAbsolutePath());

        //Simple's serializer doesn't call the setter, so we manually force a
        //parse.  Note that we are assuming that the method won't blow away
        //any items read in from the file
        ts.parseTranscript();

        return ts;
    }

    protected void writeOne(Serializer serializer, File f, TranscriptSession ts) throws Exception {
        serializer.write(ts, f);
    }
}

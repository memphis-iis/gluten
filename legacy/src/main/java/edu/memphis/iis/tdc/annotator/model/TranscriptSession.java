package edu.memphis.iis.tdc.annotator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Represents a single transcript session (persisted to an XML file). The
 * only non-vanilla work in this model class is the parsing of the transcript
 * string into the list of transcript items (which are each an instance of the
 * Utterance class).  This is generally only needed when reading a transcript
 * that has not been previously parsed and saved.
 */
@Root(name="Session")
public class TranscriptSession {
    //Note order of variables corresponds to default order when file is written

    @Element private long scriptId;
    @Element(required=false) private String tagger;
    @Element(required=false) private String verifier;
    @Element(required=false) private String taggedTime;
    @Element(required=false) private String verifiedTime;
    @Element(required=false) private String lastSavedTime;
    @Element private String beginDateTime;
//    @Element private long studentId;
//    @Element private long tutorId;
    @Element private float scriptDuration;
    @Element private float learnerLagDuration;
//    @Element private String classroom;
    @Element(required=false) private String classLevel;
    @Element(required=false) private String domain;
//    @Element(required=false) private String preSessionGrade;
//    @Element(required=false) private String postSessionGrade;
//    @Element(required=false) private float rating;
//    @Element(required=false) private int isRecommended;
//    @Element(required=false) private String gladOffers;
//    @Element(required=false) private String helpsCompleteHomework;
//    @Element(required=false) private String helpsImproveGrades;
//    @Element(required=false) private String helpsConfidence;
//    @Element(required=false) private String helpLevel;
    @Element(required=false) private String area;
    @Element(required=false) private String subarea;
//    @Element(required=false) private int achievedUnderstanding;
//    @Element(required=false) private int hadPrerequisites;
    @Element(required=false) private String problemFromLearner;
    @Element(required=false) private String learnerNotes;
    @Element(required=false) private String tutorNotes;
    @Element(required=false) private String soundness;
    @Element(required=false) private String sessionComments;
    @Element(required=false) private String learningAssessmentScore;
    @Element(required=false) private String learningAssessmentComments;
    @Element(required=false) private ModeSource modeSource;

    //Tagged lines of transcript (vs raw transcript below)
    @ElementList(required=false)
    private List<Utterance> transcriptItems = new ArrayList<Utterance>();

    //The text of a transcript - note that they can actually have a BLANK transcript
    @Element(required=false) private String transcript;

    public String getTranscript() {
        return transcript;
    }
    public void setTranscript(String transcript) {
        this.transcript = transcript;
        parseTranscript();
    }
    public String getTagger() {
        return tagger;
    }
    public void setTagger(String t) {
        this.tagger = t;
    }
    public String getVerifier() {
        return verifier;
    }
    public void setVerifier(String v) {
        this.verifier = v;
    }
    public String getTaggedTime() {
        return taggedTime;
    }
    public void setTaggedTime(String t) {
        this.taggedTime = t;
    }
    public String getLastSavedTime() {
        return lastSavedTime;
    }
    public void setLastSavedTime(String t) {
        this.lastSavedTime = t;
    }
    public String getVerifiedTime() {
        return verifiedTime;
    }
    public void setVerifiedTime(String v) {
        this.verifiedTime = v;
    }
    public long getScriptId() {
        return scriptId;
    }
    public void setScriptId(long sessionId) {
        this.scriptId = sessionId;
    }
    public String getBeginDateTime() {
        return beginDateTime;
    }
    public void setBeginDateTime(String startTime) {
        this.beginDateTime = startTime;
    }
//    public long getStudentId() {
//        return studentId;
//    }
//    public void setStudentId(long studentId) {
//        this.studentId = studentId;
//    }
//    public long getTutorId() {
//        return tutorId;
//    }
//    public void setTutorId(long tutorId) {
//        this.tutorId = tutorId;
//    }
    public float getScriptDuration() {
        return scriptDuration;
    }
    public void setScriptDuration(float sessionLength) {
        this.scriptDuration = sessionLength;
    }
    public float getLearnerLagDuration() {
        return learnerLagDuration;
    }
    public void setLearnerLagDuration(float studentWaitTime) {
        this.learnerLagDuration = studentWaitTime;
    }
//    public String getClassroom() {
//        return classroom;
//    }
//    public void setClassroom(String classroom) {
//        this.classroom = classroom;
//    }
    public String getClassLevel() {
        return classLevel;
    }
    public void setClassLevel(String gradeLevel) {
        this.classLevel = gradeLevel;
    }
    public String getDomain() {
        return domain;
    }
    public void setDomain(String subject) {
        this.domain = subject;
    }
//    public String getPreSessionGrade() {
//        return preSessionGrade;
//    }
//    public void setPreSessionGrade(String preSessionGrade) {
//        this.preSessionGrade = preSessionGrade;
//    }
//    public String getPostSessionGrade() {
//        return postSessionGrade;
//    }
//    public void setPostSessionGrade(String postSessionGrade) {
//        this.postSessionGrade = postSessionGrade;
//    }
//    public float getRating() {
//        return rating;
//    }
//    public void setRating(float rating) {
//        this.rating = rating;
//    }
//    public int getIsRecommended() {
//        return isRecommended;
//    }
//    public void setIsRecommended(int isRecommended) {
//        this.isRecommended = isRecommended;
//    }
//    public String getGladOffers() {
//        return gladOffers;
//    }
//    public void setGladOffers(String gladOffers) {
//        this.gladOffers = gladOffers;
//    }
//    public String getHelpsCompleteHomework() {
//        return helpsCompleteHomework;
//    }
//    public void setHelpsCompleteHomework(String helpsCompleteHomework) {
//        this.helpsCompleteHomework = helpsCompleteHomework;
//    }
//    public String getHelpsImproveGrades() {
//        return helpsImproveGrades;
//    }
//    public void setHelpsImproveGrades(String helpsImproveGrades) {
//        this.helpsImproveGrades = helpsImproveGrades;
//    }
//    public String getHelpsConfidence() {
//        return helpsConfidence;
//    }
//    public void setHelpsConfidence(String helpsConfidence) {
//        this.helpsConfidence = helpsConfidence;
//    }
//    public String getHelpLevel() {
//        return helpLevel;
//    }
//    public void setHelpLevel(String helpLevel) {
//        this.helpLevel = helpLevel;
//    }
    public String getArea() {
        return area;
    }
    public void setArea(String topic) {
        this.area = topic;
    }
    public String getSubarea() {
        return subarea;
    }
    public void setSubarea(String subtopic) {
        this.subarea = subtopic;
    }
//    public int getAchievedUnderstanding() {
//        return achievedUnderstanding;
//    }
//    public void setAchievedUnderstanding(int achievedUnderstanding) {
//        this.achievedUnderstanding = achievedUnderstanding;
//    }
//    public int getHadPrerequisites() {
//        return hadPrerequisites;
//    }
//    public void setHadPrerequisites(int hadPrerequisites) {
//        this.hadPrerequisites = hadPrerequisites;
//    }
    public String getProblemFromLearner() {
        return problemFromLearner;
    }
    public void setProblemFromLearner(String question) {
        this.problemFromLearner = question;
    }
    public String getLearnerNotes() {
        return learnerNotes;
    }
    public void setLearnerNotes(String studentComments) {
        this.learnerNotes = studentComments;
    }
    public String getTutorNotes() {
        return tutorNotes;
    }
    public void setTutorNotes(String tutorComments) {
        this.tutorNotes = tutorComments;
    }

    /**
     * Soundness is special - it doesn't come in the transcript sessions,
     * but we force our taggers to add it when tagging
     */
    public String getSoundness() {
        return soundness;
    }
    public void setSoundness(String soundness) {
        this.soundness = soundness;
    }

    /**
     * Like soundness, session comments don't come in the original
     * transcript sessions.  They are added by our annotators
     */
    public String getSessionComments() {
        return sessionComments;
    }
    public void setSessionComments(String s) {
        this.sessionComments = s;
    }

    /**
     * Annotator's evaluation (1-5 Likert scale) of whether or not learning occurred
     * Like soundness, not in the original transcript.
     */
    public String getLearningAssessmentScore() {
        return learningAssessmentScore;
    }
    public void setLearningAssessmentScore(String learningAssessmentScore) {
        this.learningAssessmentScore = learningAssessmentScore;
    }

    /**
     * Annotator's justification for learningAsessmentScore.
     * Like soundness, not in the original transcript.
     */
    public String getLearningAssessmentComments() {
        return learningAssessmentComments;
    }
    public void setLearningAssessmentComments(String learningAssessmentComments) {
        this.learningAssessmentComments = learningAssessmentComments;
    }

    /**
     * The mode of this transcript and the sources behind it
     */
    public ModeSource getModeSource() {
        return modeSource;
    }
    public void setModeSource(ModeSource modeSource) {
        this.modeSource = modeSource;
    }

    /**
     * Helper for modeSource examination: returns true if this transcript
     * is in <em>ANY</em> mode
     */
    public boolean isInMode() {
        return modeSource != null && StringUtils.isNotBlank(modeSource.getMode());
    }

    /**
     * Helper for modeSource examination: returns true if this transcript
     * is in the given mode
     */
    public boolean isInMode(String check) {
        return isInMode() && modeSource.getMode().equals(check);
    }

    /**
     * Helper for modeSource examination: returns true if this transcript
     * is in verification mode
     */
    public boolean isVerify() {
        return isInMode("verify");
    }

    /**
     * Helper for modeSource examination: returns true if this transcript
     * is in training mode.  <em>IMPORTANT:</em> This is currently a VIRTUAL
     * mode that we "fake" for updating the home page.  At some point, the
     * assignment process can decide if it is in training mode or not, and
     * this will become a "real" state.  Until then the edit controller decides
     * on the fly whether or not the transcript is in training mode based on
     * the existence of a matching transcript for the "trainer" user
     */
    public boolean isTraining() {
        return isInMode("training");
    }

    public List<Utterance> getTranscriptItems() {
        return transcriptItems;
    }

    /*
     * Data elements that don't correspond exactly to the raw value of an
     * XML field
     */
    private String sourceFileName;
    private String webFileName;

    /**
     * Source file name for transcript data - discovered when the file
     * is actually read
     */
    public String getSourceFileName() {
        return sourceFileName;
    }
    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    /**
     * Web file name is a string used for web displays and query strings -
     * generally only set/read by controllers/views
     */
    public String getWebFileName() {
        return webFileName;
    }
    public void setWebFileName(String webFileName) {
        this.webFileName = webFileName;
    }

    /**
     * File name extract from sourceFileName
     */
    public String getBaseFileName() {
        return FilenameUtils.getName(sourceFileName);
    }

    /**
     * Return true if every utterance has a dialog act and subact (even
     * if it's "unspecified") and at least one non-blank mode that is
     * NOT "unspecified"
     */

    public boolean isTaggingComplete() {
        List<Utterance> utts = getTranscriptItems();

        //Special - no items to tag means we're complete!
        if (utts == null || utts.size() < 1)
            return true;

        int modeCount = 0;

        for(Utterance utt: utts) {
            if (StringUtils.isBlank(utt.getDialogAct()))
                return false;
            if (StringUtils.isBlank(utt.getDialogSubAct()))
                return false;

            String mode = utt.getDialogMode();
            if (StringUtils.isNotBlank(mode) && !mode.equalsIgnoreCase("unspecified")) {
                modeCount++;
            }
        }

        //We need at least ONE unspecified mode
        return modeCount > 0;
    }

    /**
     * Parse the current transcript into transcript items.  Note that this
     * is only performed if there are not already transcriptItems.  The
     * idea is that if the list is blank then these items need to be created;
     * otherwise they are already in the file and are considered the "real"
     * transcript data
     */
    public void parseTranscript() {
        if (transcriptItems != null && transcriptItems.size() > 0) {
            return;
        }

        if (transcriptItems == null) {
            transcriptItems = new ArrayList<Utterance>();
        }
        transcriptItems.clear();

        Pattern timestamp = Pattern.compile("^\\[\\d\\d:\\d\\d:\\d\\d\\]");

        //We could actually have a blank transcript...
        if (StringUtils.isBlank(transcript)) {
            transcript = "";
            return;
        }

        boolean seenBlank = true; //Yep
        String currentSpeaker = "UKNOWN SPEAKER";

        for(String line: StringUtils.splitPreserveAllTokens(transcript, '\n')) {
            line = StringUtils.stripEnd(line, " \t\r\n");

            if (StringUtils.isBlank(line)) {
                seenBlank = true;
                continue;
            }

            Matcher match = timestamp.matcher(line.trim());

            if (match.lookingAt()) {
                //Timestamp, so an utterance
                Utterance utt = new Utterance();
                utt.setTimestamp(StringUtils.strip(match.group(), "[]"));
                utt.setText(line.substring(match.end()).trim());
                utt.setSpeaker(currentSpeaker);
                transcriptItems.add(utt);
            }
            else if (seenBlank && speakerMatch(line)){
                //No timestamp and just had a blank line - we assume that we
                //were just given the current speaker
                currentSpeaker = line.trim();
            }
            else {
                //No timestamp, but no blank line - this must be a
                //continuation of the previous timestamped line
                //Note that if we DON'T have a previous timestamped line,
                //we'll just drop this
                Utterance utt = finalUtterance();
                if (utt != null) {
                    utt.setText(utt.getText() + "\r\n" + line.trim());
                }
            }

            seenBlank = false;
        }
    }

    //Return true if the given string appear to match our "known"
    //speaker patterns
    private boolean speakerMatch(String s) {
        String check = s.trim().toLowerCase(Locale.ENGLISH);

        if (StringUtils.isBlank(check))
            return false;

        //Our order of checking is based on frequencies we saw
        //across about 2k transcripts

        //Tutor
        if (check.equals("you") || check.endsWith("(tutor)"))
            return true;

        //Student
        if (check.endsWith("(customer)"))
            return true;

        //System message
        if (check.equals("system message"))
            return true;

        return false;
    }

    private Utterance finalUtterance() {
        int sz = transcriptItems == null ? 0 : transcriptItems.size();
        if (sz < 1)
            return null;
        return transcriptItems.get(sz - 1);
    }
}

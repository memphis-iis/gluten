package edu.memphis.iis.tdc.annotator.model;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Random;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.TestService;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;
import edu.memphis.iis.tdc.annotator.model.Utterance;

//Note that the IGNORED tests here are really for churning thru bunches of
//files once in a while for load testing and comprehensive parser testing.
//Since it's 20MB compressed text, it's generally not in the repo so it
//can't be tested o a regular basis

public class TranscriptSessionTest {
    private TestService service;
    private Logger log;
    
    @Before
    public void setUp() {
        BasicConfigurator.configure();
        log = Logger.getLogger(this.getClass());
        service = new TestService();
    }
    
    @After
    public void tearDown() {
        LogManager.resetConfiguration();
    }
    
    @Test
    public void testIsBeanable() throws Exception {
        //We need to make sure the beanutils and refEquals stuff work
        //properly on this class for some of our other tests 
        URL res = getClass().getClassLoader().getResource("SampleTranscript.xml");
        TranscriptSession orig = service.getOne(res);
        TranscriptSession copy = new TranscriptSession();
        BeanUtils.copyProperties(copy, orig);
        assertTrue(tsEquals(copy, orig));
        
        //Also with the completed transcript (but ignore the items)
        res = getClass().getClassLoader().getResource("CompletedTranscript.xml");
        orig = service.getOne(res);
        orig.getTranscriptItems().clear(); //clear items first so we re-parse
        orig.setTranscript("");
        copy = new TranscriptSession();
        BeanUtils.copyProperties(copy, orig);
        assertTrue(tsEquals(copy, orig, true));
    }
    
    //Make sure that our annotator-added top-level properties (i.e. not an
    //utterance property) are working OK.  Note that the sample transcript
    //is pre-annotation so that we can add these as we go
    @Test
    public void testTopLevelCompare() throws Exception {        
        //Need 2 identical sessions for testing
        URL res = getClass().getClassLoader().getResource("SampleTranscript.xml");
        TranscriptSession orig = service.getOne(res);
        TranscriptSession copy = service.getOne(res);
        assertTrue(tsEquals(copy, orig));
        
        //Should be able to tell the difference between blank and
        //different values, and should handle across multiple sets
        
        //Soundness
        orig.setSoundness("");
        copy.setSoundness("");
        assertTrue(tsEquals(copy, orig));
        copy.setSoundness("1");
        assertFalse(tsEquals(copy, orig));
        orig.setSoundness("1");
        assertTrue(tsEquals(copy, orig));
        copy.setSoundness("3");
        assertFalse(tsEquals(copy, orig));
        orig.setSoundness("3"); //Reset for following tests
        
        //Session comments
        orig.setSessionComments("");
        copy.setSessionComments("");
        assertTrue(tsEquals(copy, orig));
        copy.setSessionComments("First Word");
        assertFalse(tsEquals(copy, orig));
        orig.setSessionComments("First Word");
        assertTrue(tsEquals(copy, orig));
        copy.setSessionComments("Last Word");
        assertFalse(tsEquals(copy, orig));
        orig.setSessionComments("Last Word"); //Reset for following tests
        
        //Learning assessment score
        orig.setLearningAssessmentScore("");
        copy.setLearningAssessmentScore("");
        assertTrue(tsEquals(copy, orig));
        copy.setLearningAssessmentScore("2");
        assertFalse(tsEquals(copy, orig));
        orig.setLearningAssessmentScore("2");
        assertTrue(tsEquals(copy, orig));
        copy.setLearningAssessmentScore("4");
        assertFalse(tsEquals(copy, orig));
        orig.setLearningAssessmentScore("4"); //Reset for following tests
        
        //Learning assessment comments
        orig.setLearningAssessmentComments("");
        copy.setLearningAssessmentComments("");
        assertTrue(tsEquals(copy, orig));
        copy.setLearningAssessmentComments("Hello");
        assertFalse(tsEquals(copy, orig));
        orig.setLearningAssessmentComments("Hello");
        assertTrue(tsEquals(copy, orig));
        copy.setLearningAssessmentComments("World");
        assertFalse(tsEquals(copy, orig));
        orig.setLearningAssessmentComments("World"); //Reset for following tests
        
        //Should survive a file rewrite
        orig.setSoundness("4");
        orig.setSessionComments("Fozzy");
        orig.setLearningAssessmentScore("5");
        orig.setLearningAssessmentComments("Kermit");
        checkReadWrite(orig);
    }
    
    @Test
    public void testIsTaggingComplete() throws Exception {
        URL res = getClass().getClassLoader().getResource("CompletedTranscript.xml");
        TranscriptSession ts = service.getOne(res);
        
        assertTrue(ts.isTaggingComplete());
        
        //Dialog Act
        ts.getTranscriptItems().get(0).setDialogAct("");
        assertFalse(ts.isTaggingComplete());
        ts.getTranscriptItems().get(0).setDialogAct("BACK!");
        assertTrue(ts.isTaggingComplete());
        
        //Dialog Sub Act
        ts.getTranscriptItems().get(0).setDialogSubAct("");
        assertFalse(ts.isTaggingComplete());
        ts.getTranscriptItems().get(0).setDialogSubAct("BACK!");
        assertTrue(ts.isTaggingComplete());
        
        //Don't care about comments
        for (Utterance u: ts.getTranscriptItems()) {
            u.setComments(null);
        }
        assertTrue(ts.isTaggingComplete());
        
        //Mode - must have at least one (and only one) that is not blank/unspecified
        for (Utterance u: ts.getTranscriptItems()) {
            u.setDialogMode("");
        }
        assertFalse(ts.isTaggingComplete());
        ts.getTranscriptItems().get(0).setDialogMode("Mode!");
        assertTrue(ts.isTaggingComplete());
        
        for (Utterance u: ts.getTranscriptItems()) {
            u.setDialogMode("unspecified");
        }
        assertFalse(ts.isTaggingComplete());
        ts.getTranscriptItems().get(ts.getTranscriptItems().size()-1).setDialogMode("Mode!");
        assertTrue(ts.isTaggingComplete());
        
        //Tag confidence - should have no effect
        Random rnd = new Random();
        for (Utterance u: ts.getTranscriptItems()) {
            u.setTagConfidence(rnd.nextInt(Integer.MAX_VALUE));
        }
        assertTrue(ts.isTaggingComplete());
        
        //Blank transcript = no lines = tagging must be done
        ts.setTranscript("");
        assertTrue(ts.isTaggingComplete());
    }
    
    @Test
    public void testTranscriptSessionReadWrite() throws Exception {
        URL res = getClass().getClassLoader().getResource("SampleTranscript.xml");
        testReadWrite(service.getOne(res));
    }
    
    private void testReadWrite(TranscriptSession orig) throws Exception {       
        //Check with blanks for tagging
        checkReadWrite(orig);
        
        int curr = 0;
        for(Utterance utt: orig.getTranscriptItems()) {
            curr++;
            utt.setDialogAct("Act:" + curr);
            utt.setDialogSubAct("SubAct:" + curr);
            utt.setDialogMode("Mode:" + curr);
            utt.setComments("Comment:" + curr);
            utt.setTagConfidence(curr);
        }
        
        //Check with utterance all set
        checkReadWrite(orig);
    }
    
    private void checkReadWrite(TranscriptSession orig) throws Exception {      
        String tmpFileName = RandomStringUtils.randomAlphabetic(16) + ".xml";
        service.writeOneTemp(orig, tmpFileName);
        TranscriptSession copy = service.getOneTemp(tmpFileName);
        FileUtils.deleteQuietly(new File(FileUtils.getTempDirectory(), tmpFileName));
        
        assertTrue(tsEquals(orig, copy));
    }
    
    //Vanilla parsing test
    @Test
    public void testTranscriptSessionParse() throws Exception {
        URL res = getClass().getClassLoader().getResource("SampleTranscript.xml");
        TranscriptSession ts = service.getOne(res);
        
        //Force parse
        ts.getTranscriptItems().clear();
        ts.parseTranscript();
        
        assertEquals(733, ts.getTranscriptItems().size());
        
        //Insure /'s on all platforms
        String fn = ts.getSourceFileName().replace('\\', '/');
        log.info("Read/Parsed: " + fn);
        assertTrue(StringUtils.endsWithIgnoreCase(fn, "/SampleTranscript.xml"));
        
        assertEquals("SampleTranscript.xml", ts.getBaseFileName());
        
        assertEquals(1, ts.getScriptId());
        assertEquals("2013-11-17T20:43:04.683-05:00", ts.getBeginDateTime()); 
       // assertEquals(1438358, ts.getStudentId());
       // assertEquals(34492, ts.getTutorId());
        assertEquals(150.28, ts.getScriptDuration(), 0.00001);
        assertEquals(0.08, ts.getLearnerLagDuration(), 0.00001);
        //assertEquals("Web", ts.getClassroom());
        assertEquals("MAT/116", ts.getClassLevel());
        assertEquals("Live Math Tutoring - MAT/116", ts.getDomain());
       // assertEquals("B", ts.getPreSessionGrade());
       // assertEquals("A", ts.getPostSessionGrade());
       // assertEquals(5.0, ts.getRating(), 0.00001);
        //assertEquals(1, ts.getIsRecommended());
       // assertEquals("Yes", ts.getGladOffers());
       // assertEquals("Yes", ts.getHelpsCompleteHomework());
        //assertEquals("Yes", ts.getHelpsImproveGrades());
       // assertEquals("Yes", ts.getHelpsConfidence());
        assertEquals("Linear Equations", ts.getArea());
        assertEquals("Solving Linear Equations", ts.getSubarea());
        //assertEquals(3, ts.getAchievedUnderstanding());
       // assertEquals(2, ts.getHadPrerequisites());
        assertEquals("confused on the gallons", ts.getProblemFromLearner());
        assertEquals("This tutor is great", ts.getLearnerNotes());
        assertEquals("This student is great", ts.getTutorNotes());
        
        assertTrue(ts.getTranscriptItems().size() > 0);
        
        List<Utterance> utts = ts.getTranscriptItems();
        assertTrue(utts.size() > 0);
        
        checkUtt(utts.get(0), "System Message", "System Message", "00:00:00", "*** Please note: All sessions are recorded for quality control. ***");
        checkUtt(utts.get(1), "KIMBERLY (Customer)", "Student", "00:00:00", "confused on the gallons");
        checkUtt(utts.get(2), "KIMBERLY (Customer)", "Student", "00:00:06", "hello");
        checkUtt(utts.get(3), "You", "Tutor", "00:00:10", "Hello, Welcome to Tutor.com!");
        
        int last = utts.size() - 1;
        checkUtt(utts.get(last-1), "KIMBERLY (Customer)", "Student", "02:30:11", "bye");
        checkUtt(utts.get(last), "You", "Tutor", "02:30:13", "bye");
    }
    
    private void checkUtt(Utterance u, String speaker, String dispSpeaker, String ts, String txt) {
        assertNotNull(u);
        assertEquals(speaker, u.getSpeaker());
        assertEquals(dispSpeaker, u.getDispSpeaker());
        assertEquals(ts, u.getTimestamp());
        assertEquals(txt, u.getText());
    }
    
    //Test transcript with line breaks in it
    @Test
    public void testParseTranscriptMultiLineUtts() throws Exception {
        URL res = getClass().getClassLoader().getResource("SampleTranscript_Wacky.xml");
        TranscriptSession ts = service.getOne(res);
        
        //Force parse
        ts.getTranscriptItems().clear();
        ts.parseTranscript();
        
        assertTrue(ts.getTranscriptItems().size() > 12);
        
        assertFalse(ts.getTranscriptItems().get(10).getText().contains("\r\n"));
        assertTrue(ts.getTranscriptItems().get(11).getText().contains("\r\n"));
    }
    
    //Test "exceptional" situations
    @Test
    public void testParseTranscriptWeirdness() throws Exception {
        URL res = getClass().getClassLoader().getResource("SampleTranscript.xml");
        TranscriptSession ts = service.getOne(res);
        TranscriptSession copy = service.getOne(res);
        
        StringBuilder sb = new StringBuilder();
        sb.append("First Person (Tutor)\n");
        sb.append("[00:00:00] hello\n");
        sb.append("[00:00:01] world\n");
        sb.append("\n");
        sb.append("Second Person (Customer)\n");
        sb.append("[00:00:02] hello\n");
        sb.append("[00:00:03] yourself\n");
        String txt = sb.toString();
        
        //Setting the transcript text shouldn't change the items
        copy.setTranscript(txt);
        assertTrue(tsEquals(ts, copy, false));
        //Make sure that the transcript compare fails 
        assertFalse(tsEquals(ts, copy, true));
        
        //Now insure that the parse succeeds with same transcript
        copy.getTranscriptItems().clear();
        copy.setTranscript(ts.getTranscript());
        assertTrue(tsEquals(ts, copy, true));
        
        //Now we can see if we actually get our test transcript
        copy.getTranscriptItems().clear();
        copy.setTranscript(txt);
        assertFalse(tsEquals(ts, copy, false));
        
        List<Utterance> utts = copy.getTranscriptItems();
        
        assertEquals(4, utts.size());
        checkUtt(utts.get(0), "First Person (Tutor)",  "Tutor", "00:00:00", "hello");
        checkUtt(utts.get(1), "First Person (Tutor)",  "Tutor", "00:00:01", "world");
        checkUtt(utts.get(2), "Second Person (Customer)", "Student", "00:00:02", "hello");
        checkUtt(utts.get(3), "Second Person (Customer)", "Student", "00:00:03", "yourself");
    }
    
    public static boolean tsEquals(TranscriptSession lhs, TranscriptSession rhs) {
        return tsEquals(lhs, rhs, true);
    }
    public static boolean tsEquals(TranscriptSession lhs, TranscriptSession rhs, boolean cmpTranscriptText) {       
        if (!(EqualsBuilder.reflectionEquals(lhs, rhs, true, null, 
                "transcript",
                "transcriptItems", 
                "sourceFileName",
                "modeSource"))) 
        {
            return false;
        }
        
        if (cmpTranscriptText && !StringUtils.equals(lhs.getTranscript(), rhs.getTranscript())) {
            return false;
        }
        
        int lhsSz = lhs.getTranscriptItems() == null ? 0 : lhs.getTranscriptItems().size();
        int rhsSz = rhs.getTranscriptItems() == null ? 0 : rhs.getTranscriptItems().size();
        
        if (lhsSz != rhsSz) {
            return false;
        }
        
        for (int i = 0; i < lhsSz; ++i) {
            Utterance lhsUtt = lhs.getTranscriptItems().get(i);
            Utterance rhsUtt = rhs.getTranscriptItems().get(i);
            if (!EqualsBuilder.reflectionEquals(lhsUtt, rhsUtt, true, null)) {
                return false;
            }
        }
        
        ModeSource lhsMode = lhs.getModeSource();
        ModeSource rhsMode = rhs.getModeSource();
        if (!EqualsBuilder.reflectionEquals(lhsMode, rhsMode, true, null, "sources")) {
            return false;
        }
        
        lhsSz = lhsMode == null ? 0 : lhsMode.getSources().size();
        rhsSz = rhsMode == null ? 0 : rhsMode.getSources().size();
        
        if (lhsSz != rhsSz) {
            return false;
        }
        
        for (int i = 0; i < lhsSz; ++i) {
            TranscriptFileInfo lhsTfi = lhsMode.getSources().get(i);
            TranscriptFileInfo rhsTfi = rhsMode.getSources().get(i);
            
            boolean eq = new EqualsBuilder()
                .append(lhsTfi.getUser(), rhsTfi.getUser())
                .append(lhsTfi.getState().toString(), rhsTfi.getState().toString())
                .append(lhsTfi.getFileName(), rhsTfi.getFileName())
                .build();

            if (!eq) {
                return false;
            }
        }
        
        return true;
    }
}

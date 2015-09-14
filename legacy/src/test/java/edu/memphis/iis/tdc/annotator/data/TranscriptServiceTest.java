package edu.memphis.iis.tdc.annotator.data;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.TestService;
import edu.memphis.iis.tdc.annotator.Utils;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.data.TranscriptService.TfiDateDescSort;
import edu.memphis.iis.tdc.annotator.model.ModeSource;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;
import static edu.memphis.iis.tdc.annotator.model.TranscriptSessionTest.tsEquals;

public class TranscriptServiceTest {
    static final String USER = "tester@test.memphis.edu";
    static final String FILE = "testCompleted.xml";
    
    private File userDir;
    private TranscriptService service;
    
    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        
        userDir = new File(
            FileUtils.getTempDirectoryPath(), 
            RandomStringUtils.randomAlphanumeric(16));
        
        //We append a separator just to go ahead and test that checkDir
        //is smart 
        Utils.checkDir(userDir.getAbsolutePath() + File.separator);
        
        service = new TranscriptService();
        service.setUserDirectory(userDir.getAbsolutePath());
        
        Logger.getLogger(this.getClass()).info("Using userDir " + service.getUserDirectory());
    }
    
    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(userDir);
        LogManager.resetConfiguration();
    }

    @Test
    public void testFileOps() throws Exception {
        //Get a test transcript, write it out, read it back, and make sure it's OK
        TranscriptSession start = readTestTranscript("CompletedTranscript.xml");
        
        service.writeTranscript(start, State.Pending, USER, FILE);
        
        TranscriptSession copy1 = service.getSingleTranscript(State.Pending, USER, FILE);
        assertTrue(tsEquals(start, copy1));
        
        service.moveTranscript(State.Pending, State.InProgress, USER, FILE);
        service.moveTranscript(State.InProgress, State.Completed, USER, FILE);
        TranscriptSession copy2 = service.getSingleTranscript(State.Completed, USER, FILE);
        assertTrue(tsEquals(copy1, copy2));
        assertTrue(tsEquals(start, copy2));
        
        assertNull(service.getSingleTranscript(State.Pending, USER, FILE));
        assertNull(service.getSingleTranscript(State.InProgress, USER, FILE));
        
        assertEquals(0, service.getUserTranscripts(State.Pending, USER).size());
        assertEquals(0, service.getUserTranscripts(State.InProgress, USER).size());
        
        List<TranscriptSession> sessions = service.getUserTranscripts(State.Completed, USER); 
        assertEquals(1, sessions.size());
        assertTrue(tsEquals(start, sessions.get(0)));
    }
    
    @Test
    public void testMoveNoOp() throws Exception {
        service.moveTranscript(State.Pending, State.Pending, USER, FILE);
        //Confirm no exceptions and file wasn't created accidentally 
        assertNull(service.getSingleTranscript(State.Pending, USER, FILE));
    }
    
    @Test
    public void testMoveAlreadyDone() throws Exception {
        TranscriptSession start = readTestTranscript("CompletedTranscript.xml");
        service.writeTranscript(start, State.InProgress, USER, FILE);
        
        service.moveTranscript(State.Pending, State.InProgress, USER, FILE);
         
        assertNull(service.getSingleTranscript(State.Pending, USER, FILE));
        
        assertTrue(tsEquals(start, service.getSingleTranscript(State.InProgress, USER, FILE)));
    }
    
    @Test(expected=IOException.class)
    public void testMoveNoFiles() throws Exception {
        service.moveTranscript(State.Pending, State.InProgress, USER, "NOT_THERE_" + FILE);
    }
    
    @Test
    public void testMoveFilesFilesEverywhere() throws Exception {
        TranscriptSession start = readTestTranscript("CompletedTranscript.xml");
        service.writeTranscript(start, State.InProgress, USER, FILE);
        service.writeTranscript(start, State.Pending, USER, FILE);
        
        service.moveTranscript(State.Pending, State.InProgress, USER, FILE);
        
        assertNull(service.getSingleTranscript(State.Pending, USER, FILE));
        assertNotNull(service.getSingleTranscript(State.InProgress, USER, FILE));
    }
    
    @Test
    public void testFindAllFiles() throws Exception {
        //Make up a bunch of files to read back
        TranscriptSession one = readTestTranscript("CompletedTranscript.xml");
        
        //Insure that our prewrite is before all files written - and make sure
        //that rounding issues on some OS's don't hurt us
        Date prewrite = new Date(new Date().getTime() - 2000);
         
        Thread.sleep(500); 
        
        service.writeTranscript(one, State.Pending, "uall", "fpending.xml");
        service.writeTranscript(one, State.InProgress, "uall", "finprogress.xml");
        service.writeTranscript(one, State.Completed, "uall", "fcompleted.xml");
        
        service.writeTranscript(one, State.Pending, "upending", "fpending.xml");
        service.writeTranscript(one, State.InProgress, "uinprogress", "finprogress.xml");
        service.writeTranscript(one, State.Completed, "ucompleted", "fcompleted.xml");

        //Insure that we found everything
        Map<String, List<TranscriptFileInfo>> all = service.findAllFiles(null, null, null);
        
        assertEquals(3, all.size());
        assertEquals(2, all.get("fpending.xml").size());
        assertEquals(2, all.get("finprogress.xml").size());
        assertEquals(2, all.get("fcompleted.xml").size());
        
        for(TranscriptFileInfo info: all.get("fpending.xml")) {
            checkOneInfo(info, "fpending.xml", State.Pending, prewrite);
        }
        
        for(TranscriptFileInfo info: all.get("finprogress.xml")) {
            checkOneInfo(info, "finprogress.xml", State.InProgress, prewrite);
        }
        
        for(TranscriptFileInfo info: all.get("fcompleted.xml")) {
            checkOneInfo(info, "fcompleted.xml", State.Completed, prewrite);
        }
        
        //Insure that all single-state searches work
        Map<String, List<TranscriptFileInfo>> some;
        
        some = service.findAllFiles(null, State.Pending, null);
        assertEquals(1, some.size());
        assertEquals(2, some.get("fpending.xml").size());
        for(TranscriptFileInfo info: all.get("fpending.xml")) {
            checkOneInfo(info, "fpending.xml", State.Pending, prewrite);
        }
        
        some = service.findAllFiles(null, State.InProgress, null);
        assertEquals(1, some.size());
        assertEquals(2, some.get("finprogress.xml").size());
        for(TranscriptFileInfo info: all.get("finprogress.xml")) {
            checkOneInfo(info, "finprogress.xml", State.InProgress, prewrite);
        }
        
        some = service.findAllFiles(null, State.Completed, null);
        assertEquals(1, some.size());
        assertEquals(2, some.get("fcompleted.xml").size());
        for(TranscriptFileInfo info: all.get("fcompleted.xml")) {
            checkOneInfo(info, "fcompleted.xml", State.Completed, prewrite);
        }
        
        //Insure single user searches work
        some = service.findAllFiles("uall", null, null);
        assertEquals(3, some.size());
        assertEquals(1, some.get("fpending.xml").size());
        assertEquals(1, some.get("finprogress.xml").size());
        assertEquals(1, some.get("fcompleted.xml").size());
        checkOneInfo(some.get("fpending.xml").get(0), "fpending.xml", State.Pending, prewrite);
        checkOneInfo(some.get("finprogress.xml").get(0), "finprogress.xml", State.InProgress, prewrite);
        checkOneInfo(some.get("fcompleted.xml").get(0), "fcompleted.xml", State.Completed, prewrite);
        
        some = service.findAllFiles("upending", State.Completed, null);
        assertEquals(0, some.size());
        
        //Insure single file searches work
        some = service.findAllFiles(null, null, "fpending.xml");
        assertEquals(1, some.size());
        assertEquals(2, some.get("fpending.xml").size());
        some = service.findAllFiles("upending", null, "fpending.xml");
        assertEquals(1, some.size());
        assertEquals(1, some.get("fpending.xml").size());
        checkOneInfo(some.get("fpending.xml").get(0), "fpending.xml", State.Pending, prewrite);
        
        //Insure that worst-case (dir not even there) single-state search works
        FileUtils.deleteDirectory(new File(userDir, "uall"));
        FileUtils.deleteDirectory(new File(userDir, "upending"));
        assertEquals(0, service.findAllFiles(null, State.Pending, null).size());
        assertEquals(0, service.findAllFiles("uall", null, null).size());
        assertEquals(0, service.findAllFiles(null, null, "fpending.xml").size());
        
        //Insure that worst-case (dir not even there) all-state search works
        FileUtils.deleteDirectory(new File(userDir, "uinprogress"));
        FileUtils.deleteDirectory(new File(userDir, "ucompleted"));
        some = service.findAllFiles(null, null, null);
        assertEquals(0, some.size());
    }
    
    private void checkOneInfo(TranscriptFileInfo info, String fn, State state, Date prewrite) {
        assertEquals(fn, info.getFileName());
        assertTrue(info.getAbsoluteFilePath().endsWith(fn));
        assertEquals(state, info.getState()); 
        
        //Sue me - I don't trust the default pre-version-8 Java date stuff
        assertNotNull(info.getLastModified());
        assertTrue(prewrite.before(info.getLastModified()));
        assertTrue(info.getLastModified().after(prewrite));
        
        assertNotNull(info.getUser());
    }
    
    @Test
    public void testModeSourceTranscript() throws Exception {
        final String FN = "fcompleted.xml";
        
        //Make up a bunch of files to read back
        TranscriptSession one = readTestTranscript("CompletedTranscript.xml");
        service.writeTranscript(one, State.Completed, "user1", FN);
        service.writeTranscript(one, State.Completed, "user2", FN);

        //Insure that we found everything
        Map<String, List<TranscriptFileInfo>> all = service.findAllFiles(null, null, null);
        assertEquals(1, all.size());
        List<TranscriptFileInfo> tfiList = all.get(FN);
        assertEquals(2, tfiList.size());
        
        ModeSource ms = new ModeSource();
        ms.setMode("verify");
        ms.getSources().add(tfiList.get(0));
        ms.getSources().add(tfiList.get(1));
        Collections.sort(ms.getSources(), new TfiDateDescSort());
        
        TranscriptSession onePre = readTestTranscript("CompletedTranscript.xml");
        assertTrue(tsEquals(one, onePre));
        onePre.setModeSource(ms);
        assertFalse(tsEquals(one, onePre));
        
        service.writeTranscript(onePre, State.Pending, "userV", FN);
        
        //Read the transcript back - use the TFI overload since that's the
        //way the majority of calls dealing with ModeSource would work
        TranscriptFileInfo tfi = new TranscriptFileInfo();
        tfi.setFileName(FN);
        tfi.setState(State.Pending);
        tfi.setUser("userV");
        TranscriptSession verifyOne = service.getSingleTranscript(tfi);
        assertTrue(tsEquals(onePre, verifyOne));
        
        //Also make sure that our read matches our manually created version
        TranscriptSession alreadyWritten = readTestTranscript("CompletedVerifyTranscript.xml");
        assertTrue(tsEquals(verifyOne, alreadyWritten));
        
        //NOTE WE'RE ABOUT TO MUTILATE THESE OBJECTS
        //Make sure we didn't get a false read
        verifyOne.getModeSource().getSources().get(0).setUser("notequal");
        assertFalse(tsEquals(onePre, verifyOne));
        verifyOne.getModeSource().getSources().remove(0);
        assertFalse(tsEquals(onePre, verifyOne));
        onePre.getModeSource().getSources().remove(0);
    }
    
    private TranscriptSession readTestTranscript(String fname) throws Exception {
        URL res = getClass().getClassLoader().getResource(fname);
        return (new TestService()).getOne(res);
    }
}

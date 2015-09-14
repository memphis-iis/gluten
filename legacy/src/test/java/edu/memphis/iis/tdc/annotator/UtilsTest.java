package edu.memphis.iis.tdc.annotator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;
import edu.memphis.iis.tdc.annotator.model.TranscriptFileInfo;

public class UtilsTest {
    private File testDir;
    private Logger log;
    
    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();
        log = Logger.getLogger(this.getClass());
        testDir = new File(
            FileUtils.getTempDirectoryPath(), 
            RandomStringUtils.randomAlphanumeric(16));
        Logger.getLogger(this.getClass()).info("testDir is " + testDir);
    }
    
    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir);
        LogManager.resetConfiguration();
    }

    @Test
    public void testStdCheckDir() {
        Utils.checkDir(testDir.getAbsolutePath() + File.separator, false);
        assertTrue(testDir.exists());
    }

    @Test
    public void testStdCheckDirCanWrite() {
        Utils.checkDir(testDir.getAbsolutePath() + File.separator, true);
        assertTrue(testDir.exists());
    }
    
    @Test(expected=RuntimeException.class)
    public void testCheckDirNoDir() {
        Utils.checkDir("");
        fail("should not be here");
    }
    
    @Test(expected=RuntimeException.class)
    public void testCheckDirInvalidDir() {
        Utils.checkDir(testDir.getAbsolutePath() + File.separator + "\\/:*?\"<>|\u0000*");
        fail("should not be here");
    }
    
    @Test
    public void testCheckDirWithTouchFile() throws Exception {
        Utils.checkDir(testDir.getAbsolutePath() + File.separator, true);
        assertTrue(testDir.exists());
        
        File touchFile = new File(testDir, "exists");
        assertFalse(touchFile.exists());
        assertTrue(touchFile.createNewFile());
        assertTrue(touchFile.exists());
        
        Utils.checkDir(testDir.getAbsolutePath(), true);
        assertTrue(testDir.exists());
    }
    
    @Test(expected=RuntimeException.class)
    public void testCheckDirWithLockedTouchFile() throws Exception {
        Utils.checkDir(testDir.getAbsolutePath() + File.separator, true);
        assertTrue(testDir.exists());
        
        File touchFile = new File(testDir, "exists");
        assertFalse(touchFile.exists());
        assertTrue(touchFile.createNewFile());
        assertTrue(touchFile.exists());
        
        FileOutputStream fos = null;
        FileLock lock = null;
        try {
            fos = new FileOutputStream(touchFile);
            lock = fos.getChannel().lock();
            Utils.checkDir(testDir.getAbsolutePath(), true);
            if (SystemUtils.IS_OS_WINDOWS) {
                //Our locking test really only runs on windowss
                fail("Should have thrown an exception");
            }
            else {
                log.warn("OS != Windows -> Locking test faked");
                throw new RuntimeException("Faking non-Win-OS lock ex");
            }
        }
        finally {
            try {
                if (lock != null) {
                    lock.release();
                }
            }
            catch(Throwable t) {
                log.error("Lock release has failed", t);
                fail("Whoops! lock release failed");
            }
            
            IOUtils.closeQuietly(fos);
        }
    }
    
    @Test
    public void testSafeInt() {
        //Make sure handle invalid strings  
        final String[] BAD_STR = new String[] {
                null, "", " ", 
                "abc", 
                "abc123", 
                "123abc",
                "42.0",
                "0.42",
                "$42"
        };
        
        final int[] DEF_CHECKS = new int[] {
                Integer.MIN_VALUE, 
                -1, 0, 1, 
                Integer.MAX_VALUE
        };
        
        for(String s: BAD_STR) {
            for (int def: DEF_CHECKS) {
                checkSafeInt(s, def, def);
                if (s == null)
                    continue;
                checkSafeInt(" " + s, def, def);
                checkSafeInt(s + " ", def, def);
                checkSafeInt("\t " + s + " \r\n", def, def);
            }
        }
        
        //Now we need to check good strings
        final String[] GOOD_STR = new String[] {"42", "+42", "-42"};
        final int[] GOOD_INT = new int[]       { 42,   42,    -42};
        int mx = Math.max(GOOD_STR.length, GOOD_INT.length); //ha! test the test
        
        for (int i = 0; i < mx; ++i) {
            String s = GOOD_STR[i];
            int x = GOOD_INT[i];
            checkSafeInt(s, 666, x);
            checkSafeInt(" " + s, 666, x);
            checkSafeInt(s + " ", 666, x);
            checkSafeInt("\t " + s + " \r\n", 666, x);
        }
    }
    private void checkSafeInt(String s, int def, int ex) {
        assertEquals("Check int string " + s, ex, Utils.safeParseInt(s, def));
    }
    
    //Suppress FindBugs warning since the verify's trip ret val check warnings
    @Test
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public void testCheckWrite() throws Exception {
        File f = mock(File.class);
        when(f.createNewFile()).thenReturn(true);
        when(f.delete()).thenReturn(true);
        Utils.checkWrite(f);
        
        verify(f).createNewFile();
        verify(f).delete();
    }
    
    @Test(expected=IOException.class)
    public void testCheckWriteCreateNoDelete() throws Exception {
        File f = mock(File.class);
        when(f.createNewFile()).thenReturn(true);
        when(f.delete()).thenReturn(false);
        Utils.checkWrite(f);
    }
    
    @Test(expected=IOException.class)
    public void testCheckWriteNoCreateDeleteFail() throws Exception {
        File f = mock(File.class);
        when(f.createNewFile()).thenReturn(false);
        when(f.delete()).thenReturn(false);
        Utils.checkWrite(f);
    }
    
    @Test(expected=IOException.class)
    public void testCheckWriteNoCreateReCreateFail() throws Exception {
        File f = mock(File.class);
        when(f.createNewFile()).thenReturn(false);
        when(f.delete()).thenReturn(true);
        when(f.delete()).thenReturn(false);
        Utils.checkWrite(f);
    }
    
    @Test(expected=IOException.class)
    public void testCheckWriteNoCreateFinalDeleteFail() throws Exception {
        File f = mock(File.class);
        when(f.createNewFile()).thenReturn(false);
        when(f.delete()).thenReturn(true);
        when(f.createNewFile()).thenReturn(true);
        when(f.delete()).thenReturn(false);
        Utils.checkWrite(f);
    }
    
    @Test
    public void testObjectToMap() {
        Map<String, Object> map;
        
        //Simple string
        map = Utils.objectToMap("Simple String");
        assertEquals(String.class, map.get("class"));
        
        //Null
        assertEquals(0, Utils.objectToMap(null).size());
        
        //More complicated - we'll just cheat and use one of our model objects
        TranscriptFileInfo tfi = new TranscriptFileInfo();
        tfi.setAbsoluteFilePath("abs");
        tfi.setFileName("file");
        tfi.setLastModified(new Date(123L));
        tfi.setState(State.Completed);
        tfi.setUser("user");
        map = Utils.objectToMap(tfi);
        log.info("Object became this map: " + map.toString());
        assertEquals("abs", map.get("absoluteFilePath"));
        assertEquals("file", map.get("fileName"));
        assertEquals(123L, ((Date)map.get("lastModified")).getTime());
        assertEquals(State.Completed, map.get("state"));
        assertEquals("user", map.get("user"));
    }
    
    @Test
    public void testNullToMap() {
        Map<String, Object> map = Utils.objectToMap(null);
        assertNotNull(map);
        assertEquals(0, map.size());
    }
}

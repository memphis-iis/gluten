package edu.memphis.iis.tdc.annotator.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.data.TranscriptService.State;

public class TranscriptFileInfoTest {
    @Test
    public void testMainCtor() {
        final String NAME = "file.txt";
        final String ABS_PATH = "/nowhere/somedir/" + NAME;
        final Long LAST_MOD = 123L;
        final State STATE = State.Pending;
        final String USR = "fozzy@muppet.com";
        final String WEB_FN = "webby";
        
        File f = mock(File.class);
        when(f.getAbsolutePath()).thenReturn(ABS_PATH);
        when(f.lastModified()).thenReturn(LAST_MOD);
        
        Date d = new Date(LAST_MOD);
        
        TranscriptFileInfo tfi = new TranscriptFileInfo(f, STATE, USR);
        assertEquals(ABS_PATH, tfi.getAbsoluteFilePath());
        assertEquals(NAME, tfi.getFileName());
        assertEquals(d, tfi.getLastModified());
        assertEquals(STATE, tfi.getState());
        assertEquals(USR, tfi.getUser());
        
        //Web file name not set by ctor and doesn't affect other file
        //name or absolute file name
        assertNull(tfi.getWebFileName());
        tfi.setWebFileName(WEB_FN);
        assertEquals(ABS_PATH, tfi.getAbsoluteFilePath());
        assertEquals(NAME, tfi.getFileName());
        assertEquals(WEB_FN, tfi.getWebFileName());
        
        TranscriptFileInfo man = new TranscriptFileInfo();
        man.setAbsoluteFilePath(ABS_PATH);
        man.setFileName(NAME);
        man.setWebFileName(WEB_FN);
        man.setLastModified(new Date(LAST_MOD));
        man.setState(STATE);
        man.setUser(USR);
        
        assertTrue(tfiEquals(tfi, man));
        
        man.setLastModified(new Date(LAST_MOD+1));
        assertFalse(tfiEquals(tfi, man));
    }
    
    private static boolean tfiEquals(TranscriptFileInfo lhs, TranscriptFileInfo rhs) {
        return EqualsBuilder.reflectionEquals(lhs, rhs, true);
    }
}

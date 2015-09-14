package edu.memphis.iis.tdc.annotator;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

//We are here strictly for silly tests involving classes we want
//included in our coverage report, but have code that won't be
//covered for some reason
//Note that we also suppress FindBugs warnings
@SuppressWarnings
public class CoverageTest {
    @Test(expected=RuntimeException.class)
    public void testConstStaticClass() {
        new Const();
        assertTrue(false); //should never get here
    }

    @Test(expected=RuntimeException.class)
    public void testUtilsStaticClass() {
        new Utils();
        assertTrue(false); //should never get here
    }
    
    @Test
    public void testTranscriptState() {
        TranscriptService.State s = TranscriptService.State.valueOf("Completed");
        assertTrue(s.equals(TranscriptService.State.valueOf("Completed")));
        assertFalse(s.equals(TranscriptService.State.valueOf("Pending")));
    }
}

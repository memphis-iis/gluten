package edu.memphis.iis.tdc.annotator.model;

import static org.junit.Assert.*;

import org.apache.commons.collections4.bag.TreeBag;
import org.junit.Before;
import org.junit.Test;

public class UtteranceTest {
    Utterance utt = null;
    
    @Before
    public void setUp() throws Exception {
        utt = new Utterance();
    }

    @Test
    public void testDispSpeaker() {
        checkDispSpeaker(null, "?");
        checkDispSpeaker("", "?");
        checkDispSpeaker(" ", "?");
        
        checkDispSpeaker("you", "Tutor");
        checkDispSpeaker("YOU", "Tutor");
        
        checkDispSpeaker("you", "Tutor");
        checkDispSpeaker(" YOU ", "Tutor");
        
        checkDispSpeaker("hello(tutor) ", "Tutor");
        checkDispSpeaker("(tutor)", "Tutor");
        checkDispSpeaker(" HAHA! (TUTOR)", "Tutor");
        
        checkDispSpeaker("hello(customer) ", "Student");
        checkDispSpeaker("(customer)", "Student");
        checkDispSpeaker(" HAHA! (CUSTOMER)", "Student");
        
        checkDispSpeaker("Sometimes vanilla is confusing", "Sometimes vanilla is confusing");
    }
    
    private void checkDispSpeaker(String speaker, String expected) {
        utt.setSpeaker(speaker);
        assertEquals(expected, utt.getDispSpeaker());
    }
    
    @Test
    public void testUttTagCompare() {
        //Use the handy apache commons bag to test
        TreeBag<Utterance> bag = new TreeBag<Utterance>(new Utterance.TagComparison());
        
        bag.add(setUtt("Act1", "SubAct1", "Mode1"));
        
        bag.add(setUtt("Act2", "SubAct1", "Mode1"));
        bag.add(setUtt("Act2", "SubAct1", "Mode1"));
        
        bag.add(setUtt("Act1", "SubAct2", "Mode1"));
        bag.add(setUtt("Act1", "SubAct2", "Mode1"));
        
        bag.add(setUtt("Act1", "SubAct1", "Mode2"));
        bag.add(setUtt("Act1", "SubAct1", "Mode2"));
        
        bag.add(setUtt("Act3", "SubAct3", "Mode3"));
        bag.add(setUtt("Act3", "SubAct3", "Mode3"));
        bag.add(setUtt("Act3", "SubAct3", "Mode3"));
        
        assertEquals(5, bag.uniqueSet().size());
        assertEquals(1, bag.getCount(setUtt("Act1", "SubAct1", "Mode1")));
        assertEquals(2, bag.getCount(setUtt("Act2", "SubAct1", "Mode1")));
        assertEquals(2, bag.getCount(setUtt("Act1", "SubAct2", "Mode1")));
        assertEquals(2, bag.getCount(setUtt("Act1", "SubAct1", "Mode2")));
        assertEquals(3, bag.getCount(setUtt("Act3", "SubAct3", "Mode3")));

    }
    
    private Utterance setUtt(String act, String sub, String mode) {
        Utterance utt = new Utterance();
        utt.setDialogAct(act);
        utt.setDialogSubAct(sub);
        utt.setDialogMode(mode);
        return utt;
    }
}

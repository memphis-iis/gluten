package edu.memphis.iis.tdc.annotator.config;

import static org.junit.Assert.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static edu.memphis.iis.tdc.annotator.config.SimpleEncrypt.*;

public class SimpleEncryptTest {
    @Before
    public void setUp() {
        BasicConfigurator.configure();
    }
    
    @After
    public void tearDown() {
        LogManager.resetConfiguration();
    }
    
    @Test
    public void testRoundTripHiding() {
        hideRoundTrip("");
        hideRoundTrip("~!@#$%^&*()_+{}|\":?><,./;''[]\\=-`");
        hideRoundTrip("0123456789");
        hideRoundTrip("abcdefghijklmnopqrstuvwxyz");
        hideRoundTrip("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        
        for(int count = 1; count <= 64; ++count) {
            for (int i = 0; i < 8; ++i) {
                String s = RandomStringUtils.randomAlphanumeric(count);
                hideRoundTrip(s);
            }
        }
    }
    
    @Test
    public void testOddities() {
        //Some strange things that we allow...
        assertEquals(hideString(""), hideString(null));
        
        assertEquals(unhideString(null), "");
        assertEquals(unhideString(""), "");
        assertEquals(unhideString(" "), "");
    }
    
    @Test(expected=RuntimeException.class)
    public void invalidHiddenString() {
        unhideString("ABCDEFG");
    }

    
    private void hideRoundTrip(String test) {
        String hidden = hideString(test);
        assertNotNull(hidden);
        String unhidden = unhideString(hidden);
        assertEquals(test, unhidden);
    }
}

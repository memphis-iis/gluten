package edu.memphis.iis.tdc.annotator;

import static org.junit.Assert.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;

public class UserErrorExceptionTest {
    @Test
    public void testMsg() {
        final String msg = "msg only test";
        
        UserErrorException before = new UserErrorException(msg);
        UserErrorException after = null;
        
        try {
            throw before;
        }
        catch(UserErrorException e) {
            after = e;
        }
        
        assertNotNull(after);
        assertTrue(before == after);
        assertEquals(msg, after.getMessage());
        assertTrue(EqualsBuilder.reflectionEquals(before, after, true));
    }

    @Test
    public void testMsgAndCause() {
        final String msg = "msg + cause test";
        
        Exception cause = new Exception("I'm the cause");
        UserErrorException before = new UserErrorException(msg, cause);
        UserErrorException after = null;
        
        try {
            throw before;
        }
        catch(UserErrorException e) {
            after = e;
        }
        
        assertNotNull(after);
        assertTrue(before == after);
        assertEquals(msg, after.getMessage());
        assertTrue(cause == after.getCause());
        assertTrue(EqualsBuilder.reflectionEquals(before, after, true));
    }
}

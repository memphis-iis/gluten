package edu.memphis.iis.tdc.annotator;

/**
 * Exception that allows our child servlets to throw up their hands and
 * send the user to an error page
 */
public class UserErrorException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public UserErrorException(String msg) {
        super(msg);
    }
    public UserErrorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

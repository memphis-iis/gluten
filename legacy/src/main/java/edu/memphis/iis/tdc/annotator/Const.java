package edu.memphis.iis.tdc.annotator;

/**
 * Provides commonly used string constants for property file names,
 * session variable names, and request attribute names.  Note that
 * we generally don't have all possibilities here - just the names
 * that are used multiple times (and in multiple places) to reduce
 * typo's and improve readability.
 */
public class Const {
    public Const() { throw new RuntimeException("Don't instantiate this class!"); }

    //Config (property) file names
    //Note that these aren't comprehensive - generally we only constant-ize
    //prop names used in multiple places
    public final static String PROP_TRAINER_NAME = "annotator.trainer.name";
    public final static String PROP_UNTAGGED_NAME = "annotator.untagged.name";
    public final static String PROP_TEST_IGNORE_USR = "annotator.test.user.notest";
    public final static String PROP_TEST_USR_EMAIL = "annotator.test.user.email";
    public final static String PROP_TEST_USR_NAME = "annotator.test.user.name";

    //Session variable names
    public final static String SESS_USR_EMAIL = "user.email";
    public final static String SESS_USR_NAME = "user.fullname";
    public final static String SESS_USR_LOC = "user.locale";
    public final static String SESS_USR_PHOTO = "user.photo";
    public final static String SESS_OAUTH_REFRESH_TOK = "refresh_token";
    public final static String SESS_OAUTH_ACCESS_TOK = "access_token";

    //Request attibute names (used by our view templates)
    public final static String REQ_USR_EMAIL = "userEmail";
    public final static String REQ_USR_NAME = "userFullName";
    public final static String REQ_USR_LOC = "userLocale";
    public final static String REQ_USR_PHOTO = "userPhoto";
    public final static String REQ_ERR_MSG = "errorMessage";
    public final static String REQ_SESS_PEND = "pendingSessions";
    public final static String REQ_SESS_INPROG = "inProgessSessions";
    public final static String REQ_SESS_COMP = "completedSessions";
    public final static String REQ_IS_ASSESSOR = "isAssessor";
    public final static String REQ_IS_ASSIGNER = "isAssigner";
    public final static String REQ_IS_VERIFIER = "isVerifier";
    public final static String REQ_TOVERIFY_LIST = "toVerifyList";
    public final static String REQ_CURRVERIFY_LIST = "currentVerifyList";
    public final static String REQ_TOASSESS_LIST = "assessList";
    public final static String REQ_TOASSIGN_LIST = "assignList";
    public final static String REQ_ASSIGN_USERS = "assignUsers";
    public final static String REQ_ASSIGN_STATES = "assignStates";
}

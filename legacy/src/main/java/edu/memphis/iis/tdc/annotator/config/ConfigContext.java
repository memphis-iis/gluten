package edu.memphis.iis.tdc.annotator.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import edu.memphis.iis.tdc.annotator.Const;
import edu.memphis.iis.tdc.annotator.Utils;
import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.model.Taxonomy;
import edu.memphis.iis.tdc.annotator.data.HttpClient;

/**
 * In this lightweight project, this Singleton is our stand in for a
 * "real" IoC container AND it's configuration
 * 
 * <p>The IMPORTANT thing to remember is that you need to specify
 * annotator.config.dir for your container, and that location should
 * have an annotator.properties file and a log4j XML config file.
 * NOTE that you can provide both of those in the classpath if you
 * wish.  Just get annotator.properties on the classpath, and inside
 * it set annotator.log4j.config.classpath to true.  See our test
 * resources in this project for an example</p>
 */
public class ConfigContext extends CompositeConfiguration {
    public static final String DEFAULT_LOG_CONFIG = "default_log4j.xml";

    private static Logger logger;
    
    private static volatile ConfigContext inst;
    
    /**
     * Get the one,true singleton instance of ConfigContext 
     */
    public static ConfigContext getInst() {
        if (inst == null)
            inst = createContext();
        return inst;
    }
    
    //Simple static setter for a static variable - gives us a place to
    //see where we manipulate a static variable from an instance method
    private static final void setLogger(Logger log) {
        logger = log;
    }
    
    /**
     * This is probably not a method you want to call.  It is for
     * overriding (or clearing) the current ConfigContext.  Generally
     * only useful for testing
     */
    public static void overwriteCurrentInstance(ConfigContext ctx) {
        inst = ctx;
    }

    /**
     * Creates a new config context.  Mainly for unit testing.
     */
    public static ConfigContext createContext() {
        return new ConfigContext();
    }
    
    private Taxonomy taxonomy;
    private ArrayList<String> taggers;
    private ArrayList<String> verifiers;
    private String derbyDir;
    private String userDataDir;
    private TranscriptService transcriptService;
    private HttpClient httpClient;
    
    //Note that if we fail to configure properly, we'll throw
    //an UNCHECKED exception
    private ConfigContext() {
        //To start, we use basic log4j configuration.  After the config file is loaded, we'll read more
        BasicConfigurator.configure();
        
        //We have a default set of properties - these may be overridden with
        //system properties (-D on the JVM command line).  In addition, we
        //load annotator.properties from directory specified at
        //annotator.config.dir - that file ALWAYS wins. 
        try {
            //Set up the default - system config beats our defaults
            CompositeConfiguration defaultConfig = new CompositeConfiguration();
            defaultConfig.setListDelimiter(',');
            defaultConfig.addConfiguration(new SystemConfiguration());
            defaultConfig.addConfiguration(new PropertiesConfiguration("annotator.default.properties"));
            
            //Read the user supplied configuration
            PropertiesConfiguration props = new PropertiesConfiguration();
            defaultConfig.setListDelimiter(',');
            props.setBasePath(defaultConfig.getString("annotator.config.dir"));
            props.setFileName("annotator.properties");
            props.load();
            
            //Now set our configurations - user-supplied beats sys/default config
            addConfiguration(props);
            addConfiguration(defaultConfig);
        }
        catch (ConfigurationException e) {
            //Yes - we're printing a stack trace - this is because we've
            //failed on basic setup, so we have no guarantee that our logging
            //will go ANYWHERE
            e.printStackTrace(); //NOPMD
            throw new RuntimeException("Could not establish base configuration", e);
        }
        
        //Configure log4j - note that we reset our configuration from the
        //basic above to use the "real" config
        LogManager.resetConfiguration();
        String logConfigFile = getString("annotator.log4j.config.file");
        if (StringUtils.isBlank(logConfigFile)) {
            //No logging config file specified - use our default file
            DOMConfigurator.configure(findResource(DEFAULT_LOG_CONFIG));
            setLogger(Logger.getLogger(ConfigContext.class));
            logger.warn("No log4j config file specified - using " + DEFAULT_LOG_CONFIG);
        }
        else {
            //They gave use the log4j config file - is it classpath or filesystem?
            //Note we assume false (so filesystem)
            if (getBoolean("annotator.log4j.config.classpath", false)) {
                DOMConfigurator.configure(findResource(logConfigFile));
                setLogger(Logger.getLogger(ConfigContext.class));
                logger.info("Used classpath log4j config file " + logConfigFile);
            }
            else {
                DOMConfigurator.configure(logConfigFile);
                setLogger(Logger.getLogger(ConfigContext.class));
                logger.info("Used filesystem log4j config file " + logConfigFile);
            }
        }
        
        //Log out our "important" configuration variables
        logger.info("annotator.config is " + getString("annotator.config"));
        logger.info("annotator.log4j.config.file is " + getString("annotator.log4j.config.file"));  
        
        if (getBoolean(Const.PROP_TEST_IGNORE_USR, false)) {
            logger.info(Const.PROP_TEST_IGNORE_USR + " detected - removing test user and email");
            clearProperty(Const.PROP_TEST_USR_EMAIL);
            clearProperty(Const.PROP_TEST_USR_NAME);
        }
        
        //Load taggers and verifiers 
        try{
        	String rolesdata = readResource("userroles.csv");
        	taggers = new ArrayList<String>();
        	verifiers = new ArrayList<String>();
        	String[] temp;
        	String[] lines=rolesdata.split("\n");
        	for(int mi=1 ; mi < lines.length ; mi++){
        		temp = lines[mi].trim().split(",");
        		if(temp.length!=2)
        			continue;
        		if(StringUtils.isBlank(temp[0]) ||StringUtils.isBlank(temp[1]) )
        			continue;
        		if(temp[1].contains("T"))
        			taggers.add(temp[0].trim().toLowerCase());
        		if(temp[1].contains("V"))
        			verifiers.add(temp[0].trim().toLowerCase());
        	}
        }
        catch (Exception e) {
            logger.fatal("Error reading userrole.csv!", e);
            throw new RuntimeException("Could not load user roles!", e);
        }
        
        
        //Load Dialog Taxonomy
        try {
            Serializer serializer = new Persister();
            taxonomy = serializer.read(Taxonomy.class, readResource("discourse_taxonomy.xml"));
        }
        catch (Exception e) {
            logger.fatal("No Discourse Taxonomy found!", e);
            throw new RuntimeException("Could not load discourse taxonomy", e);
        }
        
        //Setup data directories
        derbyDir = Utils.checkDir(reqStr("annotator.database.location"));
        userDataDir = Utils.checkDir(reqStr("annotator.transcript.dir"));
        
        //Setup services that we "inject"
        transcriptService = new TranscriptService();
        transcriptService.setUserDirectory(userDataDir);
        
        httpClient = new HttpClient();
    }
    
    /**
     * Configured taxonomy (carried in classpath)
     */
    public Taxonomy getTaxonomy() {
        return taxonomy;
    }
    
    /**
     * Location for derby database
     */
    public String getDerbyDir() {
        return derbyDir;
    }

    /**
     * Location for user directories (where the transcripts are stored)
     */
    public String getUserDataDir() {
        return userDataDir;
    }
    
    /**
     * Return true if the given user is allowed to assign transcripts 
     */
    public boolean userIsAssigner(String userEmail) {
        return userInProp(userEmail, "annotator.admin.assigner");
    }
    /**
     * Return true if based on the userrole csv the given user is a tagger 
     */
    public boolean userIsTagger(String userEmail) {    	
        return taggers.contains(userEmail);
    }    
    /**
     * Return true if based on the userrole csv the given user is allowed to verify transcripts 
     */
    public boolean userIsVerifier(String userEmail) {
    	return verifiers.contains(userEmail);
    }
    
    private boolean userInProp(String userEmail, String propName) {
        if (StringUtils.isBlank(userEmail))
            return false;
        
        String[] peeps = getStringArray(propName);
        if (peeps == null || peeps.length < 1)
            return false;
        
        for(String p: peeps) {
            if (StringUtils.equalsIgnoreCase(userEmail, p))
                return true;
        }
        return false;
    }
    
    /**
     * CRUD service for transcript files
     */
    public TranscriptService getTranscriptService() {
        return transcriptService;
    }
    
    /**
     * Client class used for HTTP comms.  STRONGLY tied to the
     * Apache commons HTTP library.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Find the given resource and return it's contents as a String.
     * Note that we assume files are in UTF-8 - this is specified in
     * our pom.xml file
     */
    public String readResource(String name) {
        if (StringUtils.isBlank(name)) {
            logger.error("There was a request for a blank resource");
            return "";
        }
        
        try {
            URL resUrl = findResource(name);
            if (resUrl == null) {
                logger.error("Could not find resource " + name);
                return "";
            }
            InputStream is = resUrl.openStream();
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            logger.error("Could not find " + name, e);
            return "";
        }
    }
    
    /**
     * Return a URL specifying the location of a resource
     */
    public URL findResource(String name) {
        return this.getClass().getClassLoader().getResource(name);
    }
    
    private String reqStr(String propName) {
        String value = StringUtils.trimToEmpty(getString(propName));
        if (StringUtils.isBlank(value)) {
            throw new RuntimeException("Required property " + propName + " was not configured");
        }
        return value;
    }
}

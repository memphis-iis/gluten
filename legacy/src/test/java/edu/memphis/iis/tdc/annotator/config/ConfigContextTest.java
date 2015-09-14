package edu.memphis.iis.tdc.annotator.config;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.model.DialogTaxTest;
import edu.memphis.iis.tdc.annotator.model.Taxonomy;

public class ConfigContextTest {    
    @Before
    public void setUp() {       
        //In case you need to debug log4j config init
        //System.setProperty("log4j.debug", "true");
        LogManager.resetConfiguration();
        ConfigContext.overwriteCurrentInstance(null);
    }
    
    @After
    public void tearDown() {
        LogManager.resetConfiguration();
        ConfigContext.overwriteCurrentInstance(null);
    }
    
    @Test
    public void testConfigLazy() {
        assertNotNull(ConfigContext.getInst()); 
    }
    
    @Test
    public void testConfigSetup() {
        ConfigContext configContext = ConfigContext.createContext();
        
        assertEquals("testing", configContext.getString("annotator.config"));
        
        assertNotNull(configContext.getDerbyDir());
        assertNotNull(configContext.getUserDataDir());
        assertNotNull(configContext.getTranscriptService());
        assertNotNull(configContext.getHttpClient());
    }
    
    @Test
    public void testAdminRoleVerifyAndTag() {
        ConfigContext configContext = ConfigContext.createContext();
        assertEquals("testing", configContext.getString("annotator.config"));
        
        //Figure out taggers and verifiers
        String tagger = null;
        String verifier = null;
        for (String line: configContext.readResource("userroles.csv").split("\n")) {
        	if (line == null || line.length() < 1)
        		continue;
        	String[] flds = line.split(",");
        	if (flds == null || flds.length != 2)
        		continue;
        	
        	if (StringUtils.isBlank(tagger) && flds[1].trim().equals("T")) {
        		tagger = flds[0].trim();
        	}
        	
        	if (StringUtils.isBlank(verifier) && flds[1].trim().equals("V")) {
        		verifier = flds[0].trim();
        	}
        	
        	if (StringUtils.isNoneBlank(tagger, verifier))
        		break;
        }
        
        assertTrue(StringUtils.isNotBlank(tagger));
        assertTrue(StringUtils.isNotBlank(verifier));
        
        //ALSO figure out non-taggers and non-verifiers
        String nonTagger = tagger.toLowerCase() + "X";
        while (configContext.userIsTagger(nonTagger)) {
        	nonTagger += "Y";
        	if (nonTagger.length() > 256) {
        		fail("Couldn't create non-tagger email");
        		return;
        	}
        }
        
        String nonVerifier = verifier.toLowerCase() + "X";
        while (configContext.userIsTagger(nonVerifier)) {
        	nonVerifier += "Y";
        	if (nonVerifier.length() > 256) {
        		fail("Couldn't create non-verifier email");
        		return;
        	}
        }
        
        assertTrue(StringUtils.isNotBlank(nonTagger));
        assertTrue(StringUtils.isNotBlank(nonVerifier));
        
        //Now we can REALLY check stuff
        assertTrue(configContext.userIsTagger(tagger));
        assertTrue(configContext.userIsVerifier(verifier));
        
        assertFalse(configContext.userIsTagger(nonTagger));
        assertFalse(configContext.userIsVerifier(nonVerifier));
                
        //Check the stupid stuff as well
        assertFalse(configContext.userIsTagger(""));
        assertFalse(configContext.userIsTagger(" "));
        assertFalse(configContext.userIsTagger(null));
        
        assertFalse(configContext.userIsVerifier(""));
        assertFalse(configContext.userIsVerifier(" "));
        assertFalse(configContext.userIsVerifier(null));
    }
    
    @Test
    public void testAdminRoleAssign() {
        ConfigContext configContext = ConfigContext.createContext();
        assertEquals("testing", configContext.getString("annotator.config"));
        
        final String EMAIL = "cnkelly@memphis.edu";
        final String DUMMY = "a@b.c";
        final String PROP = "annotator.admin.assigner";
        
        configContext.clearProperty(PROP);
        assertFalse(configContext.userIsAssigner(EMAIL));
        
        assertFalse(configContext.userIsAssigner(""));
        assertFalse(configContext.userIsAssigner(" "));
        assertFalse(configContext.userIsAssigner(null));
        
        configContext.addProperty(PROP, DUMMY);
        assertTrue(configContext.userIsAssigner(DUMMY));
        assertFalse(configContext.userIsAssigner(EMAIL));
        configContext.addProperty(PROP, EMAIL);
        assertTrue(configContext.userIsAssigner(DUMMY));
        assertTrue(configContext.userIsAssigner(EMAIL));
        
        configContext.clearProperty(PROP);
        assertFalse(configContext.userIsAssigner(EMAIL));
        assertFalse(configContext.userIsAssigner(DUMMY));
        
        configContext.addProperty(PROP, DUMMY + "," + EMAIL);
        assertTrue(configContext.userIsAssigner(DUMMY));
        assertTrue(configContext.userIsAssigner(EMAIL));
        
        assertFalse(configContext.userIsAssigner(""));
        assertFalse(configContext.userIsAssigner(" "));
        assertFalse(configContext.userIsAssigner(null));
    }
    
    @Test
    public void testDialog() {
        ConfigContext configContext = ConfigContext.createContext();
        
        Taxonomy tax = configContext.getTaxonomy();
        assertNotNull(tax);
        (new DialogTaxTest()).testTaxonomy(tax);
    }
    
    @Test
    public void testReadBadRes() {
        assertEquals("", ConfigContext.getInst().readResource(null));
        assertEquals("", ConfigContext.getInst().readResource(""));
        assertEquals("", ConfigContext.getInst().readResource(" "));
        assertEquals("", ConfigContext.getInst().readResource("/not/here/or/anywhere"));        
    }
}

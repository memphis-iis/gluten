package edu.memphis.iis.tdc.annotator.model;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import edu.memphis.iis.tdc.annotator.model.DialogAct;
import edu.memphis.iis.tdc.annotator.model.Taxonomy;

public class DialogTaxTest {
    @Test
    public void testRawDialogActs() throws Exception {
        Serializer serializer = new Persister();
        testTaxonomy(serializer.read(Taxonomy.class, readTaxFile()));
    }

    //Handy wrapper for taxonomy testing in case other tests need
    //to verify a Taxonomy reference
    public void testTaxonomy(Taxonomy tax) {
        assertNotNull(tax);
        assertTrue(tax.getDialogActs().size() > 2);
        assertTrue(tax.getDialogModes().size() > 2);
        
        for(String mode: tax.getDialogModes()) {
            assertTrue(StringUtils.isNotBlank(mode));
        }
        
        int maxSubtypes = 0;
        for(DialogAct act: tax.getDialogActs()) {
            assertTrue(StringUtils.isNotBlank(act.getName()));
            
            int stCount = act.getSubtypes().size();
            if (stCount > maxSubtypes)
                maxSubtypes = stCount;
            
            for(String st: act.getSubtypes()) {
                assertTrue(StringUtils.isNotBlank(st));
            }
        }
        
        assertTrue(maxSubtypes > 1);
    }
    
    //Mainly to make sure ourt utility function works for other tests
    @Test
    public void testIsEqual() throws Exception {
        Serializer serializer = new Persister();
        
        Taxonomy tax1 = serializer.read(Taxonomy.class, readTaxFile());
        Taxonomy tax2 = serializer.read(Taxonomy.class, readTaxFile());
        assertTrue(taxEquals(tax1, tax2));
        
        tax2.getDialogModes().add(0, "NOPE");
        assertFalse(taxEquals(tax1, tax2));
        tax2.getDialogModes().remove(0);
        assertTrue(taxEquals(tax1, tax2));
        
        tax2.getDialogActs().get(0).getSubtypes().add(0, "ROCK!");
        assertFalse(taxEquals(tax1, tax2));
        tax2.getDialogActs().get(0).getSubtypes().remove(0);
        assertTrue(taxEquals(tax1, tax2));
        tax2.getDialogActs().remove(0);
        assertFalse(taxEquals(tax1, tax2));
    }
    
    public static boolean taxEquals(Taxonomy lhs, Taxonomy rhs) {
        String lhsM = StringUtils.join(lhs.getDialogModes(), "|");
        String rhsM = StringUtils.join(rhs.getDialogModes(), "|");
        if (!StringUtils.equals(lhsM, rhsM))
            return false;
        
        int lhsSz = lhs.getDialogActs().size();
        int rhsSz = rhs.getDialogActs().size();
        if (lhsSz != rhsSz)
            return false;
        
        for(int i = 0; i < lhsSz; ++i) {
            DialogAct lhsDa = lhs.getDialogActs().get(i);
            DialogAct rhsDa = rhs.getDialogActs().get(i);
            if (!EqualsBuilder.reflectionEquals(lhsDa, rhsDa, true)) {
                return false;
            }
        }
        
        return true;
    }
    
    private InputStream readTaxFile() {
        return this.getClass().getClassLoader().getResourceAsStream("discourse_taxonomy.xml");
    }
}

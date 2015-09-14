package edu.memphis.iis.tdc.annotator.model;

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Root of the discourse taxonomy used for tagging
 * (see discourse_taxonomy.xml)
 */
@Root
public class Taxonomy {
    @ElementList private List<String> dialogModes;
    @ElementList private List<DialogAct> dialogActs;

    public List<String> getDialogModes() {
        return dialogModes;
    }
    
    public List<DialogAct> getDialogActs() {
        return dialogActs;
    }
}

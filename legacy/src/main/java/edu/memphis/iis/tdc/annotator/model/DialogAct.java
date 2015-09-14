package edu.memphis.iis.tdc.annotator.model;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Single element represent a discourse act and it's subtypes
 * (see discourse_taxonomy.xml)
 */
@Root
public class DialogAct {
    @Element
    private String name;
    
    @ElementList
    private List<String> subtypes;

    public String getName() {
        return name;
    }

    public List<String> getSubtypes() {
        return subtypes;
    }
}

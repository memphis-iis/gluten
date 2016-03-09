package edu.memphis.iis.tdc.annotator.model;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * Representation of the source for the current transcript.  Note that
 * there WON'T BE a ModeSource instance for a vanilla transcript being
 * tagged.  This is for things like verification.
 */
@Root(name="modeSource")
public class ModeSource {
    @Element private String mode;
    @ElementList private List<TranscriptFileInfo> sources;

    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<TranscriptFileInfo> getSources() {
        if (sources == null) {
            sources = new ArrayList<TranscriptFileInfo>();
        }
        return sources;
    }
    public void setSources(List<TranscriptFileInfo> sources) {
        this.sources = sources;
    }
}

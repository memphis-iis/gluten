package edu.memphis.iis.tdc.annotator.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Represents a single line in a transcript session.  Timestamp, Speaker,
 * and Text are parsed from the original transcript. All other fields are
 * user-supplied as part of the annotation process (or are calculated). 
 */
@Root
public class Utterance {
    @Element private String timestamp = "";
    @Element private String speaker = "";
    @Element(required=false) private String text = "";
    @Element(required=false) private String dialogAct = "";
    @Element(required=false) private String dialogSubAct = "";
    @Element(required=false) private String dialogMode = "";
    @Element(required=false) private String comments = "";
    @Element(required=false) private int tagConfidence = 1;
    
    public static class TagComparison implements Comparator<Utterance>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public int compare(Utterance o1, Utterance o2) {
            return new CompareToBuilder()
                .append(o1.getDialogAct(), o2.getDialogAct())
                .append(o1.getDialogSubAct(), o2.getDialogSubAct())
                .append(o1.getDialogMode(), o2.getDialogMode())
                .toComparison();
        }
    };
    
    /**
     * From transcript - the time of the utterance (relative to start of session)
     */
    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * From transcript - string specify who generated this utterance
     */
    public String getSpeaker() {
        return speaker;
    }
    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }
    
    /**
     * Provide a "displayable" version of speaker.  Generally this handles
     * exception cases, anonymity, shortening, etc
     */
    public String getDispSpeaker() {
        if (StringUtils.isBlank(speaker)) {
            return "?";
        }
        
        String check = speaker.trim().toLowerCase(Locale.ENGLISH);
        if (check.equals("you")) {
            return "Tutor";
        }
        else if (check.endsWith("(customer)")) {
            return "Student";
        }
        else if (check.endsWith("(tutor)")) {
            return "Tutor";
        }
        
        return speaker;
    }
    
    /**
     * From transcript - Actual text of utterance
     */
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * User entered - Dialog act (from our taxonomy)
     */
    public String getDialogAct() {
        return dialogAct;
    }
    public void setDialogAct(String dialogAct) {
        this.dialogAct = dialogAct;
    }
    
    /**
     * User entered - Dialog sub-act (from our taxonomy) - dependent on
     * user-specified DialogAct 
     */
    public String getDialogSubAct() {
        return dialogSubAct;
    }
    public void setDialogSubAct(String dialogSubAct) {
        this.dialogSubAct = dialogSubAct;
    }
    
    /**
     * User entered - Dialog mdoe (from our taxonomy)
     */
    public String getDialogMode() {
        return dialogMode;
    }
    public void setDialogMode(String dialogMode) {
        this.dialogMode = dialogMode;
    }
    
    /**
     * User entered - comments about the current utterance
     */
    public String getComments() {
        return comments;
    }
    public void setComments(String comments) {
        this.comments = comments;
    }
    
    /**
     * User may specify whether or not they are confident with their tag 
     */
    public int getTagConfidence() {
        return tagConfidence;
    }
    public void setTagConfidence(int tagConfidence) {
        this.tagConfidence = tagConfidence;
    }
    
    /**
     * Helper: return string representation of tag state for comparisons
     */
    public String getTagRepr() {
        return String.format("%s:%s:%s", dialogAct, dialogSubAct, dialogMode);
    }
}

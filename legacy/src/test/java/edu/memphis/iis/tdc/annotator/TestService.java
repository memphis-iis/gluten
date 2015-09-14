package edu.memphis.iis.tdc.annotator;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import edu.memphis.iis.tdc.annotator.data.TranscriptService;
import edu.memphis.iis.tdc.annotator.model.TranscriptSession;

//Simple test wrapper around TranscriptService that allows us to parse a
//transcript file from other places
public class TestService extends TranscriptService {
    private Logger log = Logger.getLogger(TestService.class);
    
    public TranscriptSession getOne(URL u) throws Exception {
        return parseOne(createSerializer(), new File(u.getFile()));
    }
    
    public TranscriptSession getOne(File f) throws Exception {
        return parseOne(createSerializer(), f);
    }
    
    public TranscriptSession getOneTemp(String tmpFileName) throws Exception {
        File f = new File(FileUtils.getTempDirectory(), tmpFileName);
        log.info("READING " + f.getAbsolutePath());
        return getOne(f);
    }
    
    public void writeOneTemp(TranscriptSession ts, String tmpFileName) throws Exception {
        File f = new File(FileUtils.getTempDirectory(), tmpFileName);
        log.info("WRITING " + f.getAbsolutePath());
        writeOne(createSerializer(), f, ts);
    }
    
    public void writeOneDirect(TranscriptSession ts, File f) throws Exception {
        writeOne(createSerializer(), f, ts);
    }
}
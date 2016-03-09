package edu.memphis.iis.tdc.annotator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Small class for simple utility classes
 */
public class Utils {
    private final static Logger logger = Logger.getLogger(Utils.class);

    public Utils() { throw new RuntimeException("Don't instantiate this class!"); }

    /**
     * Make sure the given directory is valid, that it exists, and that it is
     * writable (by creating and deleting a "touch" file)
     * @param dirName name to check
     * @param checkForWrite if true, insure that the directory can be written to
     * @return name with any changes
     * @throws RuntimeException if the directory can't be created/checked
     */

    public static String checkDir(String dirName, boolean checkForWrite) {
        while(dirName.endsWith(File.separator)) {
            dirName = dirName.substring(0, dirName.length() - File.separator.length());
        }

        if (StringUtils.isBlank(dirName)) {
            throw new RuntimeException("Invalid directory name discovered");
        }

        try {
            File touchFile = new File(dirName + "/exists");
            FileUtils.forceMkdir(touchFile.getParentFile());
            if (checkForWrite) {
                checkWrite(touchFile);
            }
        }
        catch (Exception e) {
            String errMsg = "Problem setting up for directory " + dirName;
            logger.fatal(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }

        return dirName;
    }

    public static void checkWrite(File touchFile) throws IOException {
        if (!touchFile.createNewFile()) {
            //File was already there? try to delete and recreate
            if (!touchFile.delete()) {
                throw new IOException("Previous touch file found and deletion failed but threw no exception");
            }
            if (!touchFile.createNewFile()) {
                throw new IOException("Touch file exists after delete?");
            }
        }

        if (!touchFile.delete()) {
            throw new IOException("Touch file deletion failed but threw no exception");
        }
    }

    /**
     * Overloaded method: calls checkDir with checkForWrite = true
     */
    public static String checkDir(String dirName) {
        return checkDir(dirName, true);
    }
    /**
     * Handle int conversions without exceptions
     */
    public static int safeParseInt(String s, int defaultVal) {
        if (StringUtils.isBlank(s))
            return defaultVal;
        try {
            return Integer.parseInt(s.trim());
        }
        catch(Throwable t) {
            return defaultVal;
        }
    }

    /**
     * Using the commons beanutils and a little wrapper, create a map
     * from property name to value object
     */
    public static Map<String, Object> objectToMap(Object obj) {
        BeanMap beanMap = new BeanMap(obj);
        Map<String, Object> map = new HashMap<String, Object>();

        //yes, archaic iterator code, but the BeanMap is so handy...
        Iterator<String> keys = beanMap.keyIterator();
        while(keys.hasNext()) {
            String key = keys.next();
            map.put(key, beanMap.get(key));
        }

        return map;
    }
}

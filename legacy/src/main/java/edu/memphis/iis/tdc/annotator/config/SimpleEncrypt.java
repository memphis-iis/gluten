package edu.memphis.iis.tdc.annotator.config;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Provide VERY simple (and not 100% secure) string encryption/decryption.
 * This is really only useful for simple string "hiding" for URL's where
 * we don't really care if a hacker with a cloud account and 2 days to
 * kill can crack the querystring.
 */
public class SimpleEncrypt {
    private static final Logger logger = Logger.getLogger(SimpleEncrypt.class);

    //Just created with random.org
    //Note that we are using hard-coded, plaintext stuff for our encryption.
    //DON'T DO THIS FOR REAL ENCRYPTION
    private static final char[] hideKeyPwd = "X3RIFA9WN78TIVtrZ5g7F0zWXzXw9bRBj".toCharArray();
    private static final byte[] hideKeySalt = "pkOZDsNo".getBytes(StandardCharsets.UTF_8);
    private static final byte[] hideKeyIv = "XjMKV66y2C0sKHVG".getBytes(StandardCharsets.UTF_8);

    //Setting up ciphers is time consuming, so we use a thread-local cipher
    //for a small performance tweak (since hide/unhide string could get
    //called 100's of times for a single page render)
    //NOTE that this will cause Memory Leak messages in the Tomcat logs - 99%
    //of the time that would be true for the pattern we're using, but we
    //actually WANT to leave these attached to their threads

    private static ThreadLocal<Cipher> encryptCipher = new ThreadLocal<Cipher>() {
        protected Cipher initialValue() {
            try {
                return getCipher(Cipher.ENCRYPT_MODE);
            }
            catch(Exception e) {
                return null;
            }
        }
    };

    private static ThreadLocal<Cipher> decryptCipher = new ThreadLocal<Cipher>() {
        protected Cipher initialValue() {
            try {
                return getCipher(Cipher.DECRYPT_MODE);
            }
            catch(Exception e) {
                return null;
            }
        }
    };

    private static Cipher getCipher(int optMode) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(hideKeyPwd, hideKeySalt, 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(optMode, secret, new IvParameterSpec(hideKeyIv));

        return cipher;
    }

    /**
     * Used for "hiding" a string that we want to send to the browser
     * (for instance, something that will be part of a URL but we don't
     * want the user to see).  NOTE that you shouldn't assume that these
     * string are perfectly cryptographically secure.
     *
     * Also note that a failure here is logged as a fatal - AND an unchecked
     * exception is thrown.  Failing here is bad mojo.
     */
    public static String hideString(String src) {
        try {
            if (src == null)
                src = "";

            //encrypt
            byte[] todo = src.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = encryptCipher.get().doFinal(todo);

            //Remember that base64 encodes to US ASCII codes
            return StringUtils.toEncodedString(
                    Base64.encodeBase64URLSafe(encrypted),
                    StandardCharsets.US_ASCII);
        }
        catch (Throwable e) {
            String errMsg = "Could not encrypt " + src + " for hiding";
            logger.fatal(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    /**
     * "Unhides" a string hidden with hideString.  Note that if src is blank
     * (null, empty, or whitespace only), then an empty string is returned.
     */
    public static String unhideString(String src) {
        if (StringUtils.isBlank(src)) {
            return "";
        }

        try {
            byte[] todo = Base64.decodeBase64(src);
            byte[] decrypted = decryptCipher.get().doFinal(todo);
            return StringUtils.toEncodedString(decrypted, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            String errMsg = "Could not decrypt " + src + " from hiding";
            logger.fatal(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }
}

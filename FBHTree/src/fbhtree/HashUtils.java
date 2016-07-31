package fbhtree;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Scott
 */
public class HashUtils {
    public static final Logger LOG;
    public static final char[] HEX_CHARS;
    
    static {
        LOG = Logger.getLogger(HashUtils.class.getName());
        HEX_CHARS = "0123456789abcdef".toCharArray();
    }
    
    public static String byte2hex(byte[] bytes) {
        return byte2HEX(bytes).toLowerCase();
    }
    
    public static String byte2HEX(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }
    
    public static byte[] hex2byte(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }

    
    public static byte[] sha256(byte[]... bytesArr) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            for (byte[] bytes: bytesArr) {
                md.update(bytes);
            }
            
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            LOG.log(Level.SEVERE, null, ex);
            
            return null;
        }
    }
    
    public static byte[] sha256(Collection<byte[]> bytesCollection) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            for (byte[] bytes: bytesCollection) {
                md.update(bytes);
            }
            
            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            LOG.log(Level.SEVERE, null, ex);
            
            return null;
        }
    }
    
    public static String sha256(String data) {
        return byte2hex(data.getBytes());
    }
    
    public static String sha256(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            
            try (DigestInputStream dis = new DigestInputStream(is, md)) {
                while (dis.read(buffer) != -1);
            }

            return byte2hex(md.digest());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            
            return null;
        }
    }
}
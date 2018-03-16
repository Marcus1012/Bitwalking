package bitwalking.bitwalking.security;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import bitwalking.bitwalking.BitwalkingApp;

/**
 * Created by Marcus on 9/3/16.
 */
public enum SessionInfo {
    INSTANCE;

    private static final int RSA_KEY_SIZE = 1024;

    private Key SRVpub = null;   // Server public key
    private Key CLIpub = null;   // Client public key
    private Key CLIpriv = null;  // Client private key
    private SecretKey SK = null; // Session key

    SessionInfo() {

    }

    //region Server Public Key
    public Key getSRVpub() {
        return SRVpub;
    }

    public void setSRVpub(byte[] newSRVpub) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SRVpub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(newSRVpub));
    }
    //endregion

    //region Client Keys
    public Key getCLIpub() {
        return CLIpub;
    }

    public Key getCLIpriv() {
        return CLIpriv;
    }

    public void generateCLIkeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();

            CLIpub = keyPair.getPublic();
            CLIpriv = keyPair.getPrivate();
        }
        catch(Exception e) {
            BitwalkingApp.getInstance().trackException(new Exception("failed generating CLI keys", e));
        }
    }
    //endregion

    //region RSA Helpers

    public byte[] RSAEncrypt(final String plain, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(plain.getBytes());

        return encryptedBytes;
    }

    public String RSADecrypt(final byte[] encryptedBytes, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }

    //endregion

    //region Session Key

    public SecretKey getSK() {
        return SK;
    }

    public void setSK(byte[] newSK) {
        SK = new SecretKeySpec(newSK, 0, newSK.length, "AES");
    }

    //endregion

    //region Session Helpers

    public byte[] SessionEncrypt(final String plain, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(plain.getBytes("UTF-8"));
    }

    public String SessionDecrypt(final byte[] encryptedBytes, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }

    //endregion
}

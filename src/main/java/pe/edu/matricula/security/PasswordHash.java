package pe.edu.matricula.security;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class PasswordHash {
    private static final int ITERATIONS=600000;
    private PasswordHash(){}
    public static String hash(char[] password){
        if(password==null||password.length<12||password.length>128)throw new IllegalArgumentException("Use una contraseña de 12 a 128 caracteres.");
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2_sha256$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(password,salt,ITERATIONS));
    }
    public static boolean verify(char[] password,String stored){
        if(password==null||password.length>128||stored==null)return false;
        try{
            String[] p=stored.split("\\$",-1);
            if(p.length!=4||!p[0].equals("pbkdf2_sha256"))return false;
            int iterations=Integer.parseInt(p[1]);if(iterations<100000||iterations>1000000)return false;
            byte[] salt=Base64.getDecoder().decode(p[2]),expected=Base64.getDecoder().decode(p[3]);
            if(salt.length!=16||expected.length!=32)return false;
            return MessageDigest.isEqual(expected,derive(password,salt,iterations));
        }catch(IllegalArgumentException e){return false;}
    }
    private static byte[] derive(char[] password,byte[] salt,int iterations){
        PBEKeySpec spec=new PBEKeySpec(password,salt,iterations,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(GeneralSecurityException e){throw new IllegalStateException(e);}
        finally{spec.clearPassword();}
    }
}

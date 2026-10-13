package pe.edu.matricula.security;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
public final class Tokens {
    private Tokens(){}
    public static String create(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public static boolean matches(String expected,String actual){return expected!=null&&actual!=null&&actual.length()==expected.length()&&MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),actual.getBytes(StandardCharsets.UTF_8));}
}

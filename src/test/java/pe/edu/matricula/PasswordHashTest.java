package pe.edu.matricula;
import org.junit.jupiter.api.Test;
import pe.edu.matricula.security.*;
import static org.junit.jupiter.api.Assertions.*;
public class PasswordHashTest {
 @Test void verificaSinGuardarLaClaveOriginal(){char[] p="ClaveEscolarDePrueba123!".toCharArray();String h=PasswordHash.hash(p);assertFalse(h.contains("ClaveEscolar"));assertTrue(PasswordHash.verify(p,h));assertFalse(PasswordHash.verify("OtraClave123!".toCharArray(),h));}
 @Test void cadaCuentaRecibeUnaSalDiferente(){char[] p="ClaveEscolarDePrueba123!".toCharArray();assertNotEquals(PasswordHash.hash(p),PasswordHash.hash(p));}
 @Test void hashCorruptoNoIniciaSesion(){assertFalse(PasswordHash.verify("ClaveEscolar123!".toCharArray(),"pbkdf2_sha256$999999999$x$x"));}
 @Test void tokenFaltanteODiferenteNoPasa(){String t=Tokens.create();assertTrue(Tokens.matches(t,t));assertFalse(Tokens.matches(t,null));assertFalse(Tokens.matches(t,Tokens.create()));}
}

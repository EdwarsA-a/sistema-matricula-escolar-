package pe.edu.matricula;
import org.junit.jupiter.api.Test;
import pe.edu.matricula.service.*;
import static org.junit.jupiter.api.Assertions.*;
public class ReglasTest {
 @Test void pendienteNoSeRetiraComoActiva(){assertThrows(BusinessException.class,()->Reglas.transition("PENDIENTE","RETIRO"));}
 @Test void anuladaNoPuedeReactivarse(){assertThrows(BusinessException.class,()->Reglas.transition("ANULADA","ACTIVACION"));}
 @Test void activaNoSeActivaDeNuevo(){assertThrows(BusinessException.class,()->Reglas.transition("ACTIVA","ACTIVACION"));}
 @Test void pendienteSeActiva(){assertDoesNotThrow(()->Reglas.transition("PENDIENTE","ACTIVACION"));}
 @Test void seccionLlenaRechazaUnaMatriculaMas(){assertThrows(BusinessException.class,()->Reglas.capacity(30,30));}
 @Test void vacanteDisponiblePermiteConfirmar(){assertDoesNotThrow(()->Reglas.capacity(29,30));}
 @Test void identificadorNoAdmiteCeroNiSql(){assertThrows(BusinessException.class,()->Reglas.id("0","estudiante"));assertThrows(BusinessException.class,()->Reglas.id("1 OR 1=1","estudiante"));}
}

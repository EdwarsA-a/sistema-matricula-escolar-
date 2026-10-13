package pe.edu.matricula;
import org.junit.jupiter.api.Test;
import pe.edu.matricula.dao.SqlDAO;
import pe.edu.matricula.service.BusinessException;
import java.sql.Connection;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
public class TransaccionTest {
 private Connection connection(AtomicInteger commits,AtomicInteger rollbacks){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{if(m.getName().equals("commit"))commits.incrementAndGet();if(m.getName().equals("rollback"))rollbacks.incrementAndGet();if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;});}
 @Test void validaConfirmaUnaVez()throws Exception{AtomicInteger commits=new AtomicInteger(),rollbacks=new AtomicInteger();SqlDAO dao=new SqlDAO(()->connection(commits,rollbacks));assertEquals(42,dao.transaction(c->42).intValue());assertEquals(1,commits.get());assertEquals(0,rollbacks.get());}
 @Test void reglaRechazadaRevierteTodaLaOperacion(){AtomicInteger commits=new AtomicInteger(),rollbacks=new AtomicInteger();SqlDAO dao=new SqlDAO(()->connection(commits,rollbacks));assertThrows(BusinessException.class,()->dao.transaction(c->{throw new BusinessException("Sin vacante");}));assertEquals(0,commits.get());assertEquals(1,rollbacks.get());}
}

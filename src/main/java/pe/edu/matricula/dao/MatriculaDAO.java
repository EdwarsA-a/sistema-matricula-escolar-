package pe.edu.matricula.dao;
import java.sql.*;
import java.util.*;
import pe.edu.matricula.service.Reglas;
public final class MatriculaDAO {
    private final SqlDAO sql=new SqlDAO();
    public Map<String,Object> periodo(Connection c,long id)throws SQLException{
        var p=sql.one(c,"SELECT * FROM periodos WHERE id=? FOR UPDATE",id);
        Reglas.ensure(p!=null,"El periodo no existe.");return p;
    }
    public Map<String,Object> seccion(Connection c,long id)throws SQLException{
        var s=sql.one(c,"SELECT * FROM secciones WHERE id=? FOR UPDATE",id);
        Reglas.ensure(s!=null&&SqlDAO.flag(s,"activa"),"La sección no existe o está inactiva.");return s;
    }
    public Map<String,Object> matricula(Connection c,long id,boolean lock)throws SQLException{
        var m=sql.one(c,"SELECT * FROM matriculas WHERE id=?"+(lock?" FOR UPDATE":""),id);
        Reglas.ensure(m!=null,"La matrícula no existe.");return m;
    }
    public long ocupadas(Connection c,long section)throws SQLException{
        return SqlDAO.number(sql.one(c,"SELECT COUNT(*) AS cantidad FROM matriculas WHERE seccion_id=? AND estado='ACTIVA'",section),"cantidad");
    }
    public void movimiento(Connection c,long id,String tipo,String anterior,String nuevo,Long origen,Long destino,String motivo,long user)throws SQLException{
        sql.execute(c,"INSERT INTO movimientos_matricula(matricula_id,tipo,estado_anterior,estado_nuevo,seccion_anterior_id,seccion_nueva_id,motivo,usuario_id) VALUES(?,?,?,?,?,?,?,?)",id,tipo,anterior,nuevo,origen,destino,motivo,user);
    }
}

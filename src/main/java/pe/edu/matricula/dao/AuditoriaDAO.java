package pe.edu.matricula.dao;
import java.sql.*;
import java.util.Map;
public final class AuditoriaDAO {
    private final SqlDAO sql=new SqlDAO();
    public void register(Connection c,long user,String action,String entity,Long id,Object before,Object after)throws SQLException{
        String detail="{\"anterior\":"+json(before)+",\"nuevo\":"+json(after)+"}";
        sql.execute(c,"INSERT INTO auditoria(usuario_id,accion,entidad,registro_id,detalle) VALUES(?,?,?,?,?)",user,action,entity,id,detail);
    }
    private String json(Object o){
        if(o==null)return "null";
        if(o instanceof Map<?,?> m){StringBuilder s=new StringBuilder("{");boolean first=true;for(var e:m.entrySet()){if(!first)s.append(',');first=false;s.append(quote(e.getKey().toString())).append(':').append(json(e.getValue()));}return s.append('}').toString();}
        if(o instanceof Number||o instanceof Boolean)return o.toString();
        return quote(o.toString());
    }
    private String quote(String text){
        StringBuilder s=new StringBuilder("\"");
        for(char c:text.toCharArray())switch(c){case '"'->s.append("\\\"");case '\\'->s.append("\\\\");case '\n'->s.append("\\n");case '\r'->s.append("\\r");case '\t'->s.append("\\t");default->{if(c<32)s.append(String.format("\\u%04x",(int)c));else s.append(c);}}
        return s.append('"').toString();
    }
}

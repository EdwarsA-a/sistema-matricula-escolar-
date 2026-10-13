package pe.edu.matricula.dao;

import pe.edu.matricula.config.ConexionFactory;
import java.sql.*;
import java.util.*;

/** SQL fijo en el servidor y valores siempre parametrizados. */
public final class SqlDAO {
    @FunctionalInterface public interface Provider { Connection open() throws SQLException; }
    private final Provider provider;
    public SqlDAO(){this(ConexionFactory::open);}
    public SqlDAO(Provider provider){this.provider=provider;}
    @FunctionalInterface public interface Work<T> { T run(Connection c) throws SQLException; }
    public <T> T transaction(Work<T> work) throws SQLException {
        try (Connection c = provider.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);
            try { T result = work.run(c); c.commit(); return result; }
            catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }
    public List<Map<String,Object>> list(String sql, Object... values) throws SQLException {
        try (Connection c=provider.open()) { return list(c,sql,values); }
    }
    public Map<String,Object> one(String sql, Object... values) throws SQLException {
        try (Connection c=provider.open()) { return one(c,sql,values); }
    }
    public List<Map<String,Object>> list(Connection c,String sql,Object... values) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            bind(p,values);
            try(ResultSet r=p.executeQuery()) {
                List<Map<String,Object>> rows=new ArrayList<>();
                ResultSetMetaData md=r.getMetaData();
                while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=md.getColumnCount();i++) row.put(md.getColumnLabel(i),r.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    public Map<String,Object> one(Connection c,String sql,Object... values) throws SQLException {
        List<Map<String,Object>> rows=list(c,sql,values);return rows.isEmpty()?null:rows.get(0);
    }
    public long insert(Connection c,String sql,Object... values) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            bind(p,values);p.executeUpdate();
            try(ResultSet r=p.getGeneratedKeys()) { if(r.next())return r.getLong(1);throw new SQLException("No se obtuvo el identificador."); }
        }
    }
    public int execute(Connection c,String sql,Object... values) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {bind(p,values);return p.executeUpdate();}
    }
    private void bind(PreparedStatement p,Object[] v)throws SQLException {for(int i=0;i<v.length;i++)p.setObject(i+1,v[i]);}
    public static long number(Map<String,Object> row,String field){return ((Number)row.get(field)).longValue();}
    public static boolean flag(Map<String,Object> row,String field) {
        Object v=row.get(field);return v instanceof Boolean b?b:v instanceof Number n && n.intValue()!=0;
    }
}

package pe.edu.matricula.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Una conexion por operacion; las transacciones comparten esa conexion. */
public final class ConexionFactory {
    private ConexionFactory() {}
    public static Connection open() throws SQLException {
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) throw new SQLException("Configure DB_PASSWORD antes de iniciar el servidor.");
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/matricula_escolar?serverTimezone=America/Lima&useSSL=false&allowPublicKeyRetrieval=true");
        String user = System.getenv().getOrDefault("DB_USER", "matricula_app");
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new SQLException("Falta MySQL Connector/J.", e); }
        return DriverManager.getConnection(url, user, password);
    }
}

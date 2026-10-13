package pe.edu.matricula.tools;
import pe.edu.matricula.dao.*;
import pe.edu.matricula.security.PasswordHash;
import java.util.*;
public final class BootstrapAdmin {
    public static void main(String[] args)throws Exception{
        String password=System.getenv("ADMIN_PASSWORD");
        if(password==null||password.isBlank())throw new IllegalArgumentException("Defina ADMIN_PASSWORD temporalmente para crear la primera cuenta.");
        String username=System.getenv().getOrDefault("ADMIN_USERNAME","admin");
        char[] chars=password.toCharArray();String hash;
        try{hash=PasswordHash.hash(chars);}finally{Arrays.fill(chars,'\0');}
        SqlDAO sql=new SqlDAO();sql.transaction(c->{
            var existing=sql.one(c,"SELECT COUNT(*) AS cantidad FROM usuarios");
            if(SqlDAO.number(existing,"cantidad")>0)throw new IllegalStateException("Ya hay usuarios. Cree nuevas cuentas desde la pantalla de administración.");
            var role=sql.one(c,"SELECT id FROM roles WHERE nombre='ADMIN'");if(role==null)throw new IllegalStateException("Ejecute database/02_catalogos.sql primero.");
            long id=sql.insert(c,"INSERT INTO usuarios(rol_id,nombre,username,password_hash) VALUES(?,?,?,?)",SqlDAO.number(role,"id"),"Administrador",username,hash);
            new AuditoriaDAO().register(c,id,"INICIALIZAR_ADMIN","usuarios",id,null,Map.of("username",username));return null;
        });System.out.println("Cuenta administradora creada. Usuario: "+username);
    }
}

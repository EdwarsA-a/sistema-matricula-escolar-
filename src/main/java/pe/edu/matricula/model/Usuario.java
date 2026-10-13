package pe.edu.matricula.model;
import java.io.Serializable;
public record Usuario(long id,String nombre,String username,String rol) implements Serializable {
    public long getId(){return id;} public String getNombre(){return nombre;}
    public String getUsername(){return username;} public String getRol(){return rol;}
    public boolean getAdministrador(){return "ADMIN".equals(rol);}
    public boolean getOperador(){return "ADMIN".equals(rol)||"MATRICULA".equals(rol);}
}

package pe.edu.matricula.service;
import pe.edu.matricula.dao.*;
import pe.edu.matricula.model.Usuario;
import pe.edu.matricula.security.PasswordHash;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import static pe.edu.matricula.service.Reglas.*;
import static pe.edu.matricula.dao.SqlDAO.*;
public final class ConfiguracionService {
    private final SqlDAO sql=new SqlDAO();private final AuditoriaDAO audit=new AuditoriaDAO();
    private void admin(Usuario u){ensure(u.getAdministrador(),"Esta acción requiere el rol administrador.");}
    public void periodo(Usuario u,Long id,String code,String start,String end,String state)throws SQLException{
        admin(u);String codigo=required(code,20,"Código del periodo");LocalDate begin=date(start,"Inicio"),finish=date(end,"Fin");
        ensure(!finish.isBefore(begin),"La fecha final no puede ser anterior al inicio.");ensure(Set.of("PLANIFICADO","ABIERTO","CERRADO").contains(state),"Estado de periodo inválido.");
        sql.transaction(c->{var before=id==null?null:sql.one(c,"SELECT * FROM periodos WHERE id=? FOR UPDATE",id);ensure(id==null||before!=null,"El periodo no existe.");
            long key;if(id==null)key=sql.insert(c,"INSERT INTO periodos(codigo,fecha_inicio,fecha_fin,estado) VALUES(?,?,?,?)",codigo,java.sql.Date.valueOf(begin),java.sql.Date.valueOf(finish),state);
            else{key=id;sql.execute(c,"UPDATE periodos SET codigo=?,fecha_inicio=?,fecha_fin=?,estado=? WHERE id=?",codigo,java.sql.Date.valueOf(begin),java.sql.Date.valueOf(finish),state,id);}
            audit.register(c,u.id(),id==null?"CREAR":"ACTUALIZAR","periodos",key,before,sql.one(c,"SELECT * FROM periodos WHERE id=?",key));return null;});
    }
    public void seccion(Usuario u,Long id,long period,long grade,String name,long cap,boolean active)throws SQLException{
        admin(u);String section=required(name,20,"Nombre de sección");ensure(cap>0&&cap<=1000,"La capacidad debe estar entre 1 y 1000.");
        sql.transaction(c->{var p=sql.one(c,"SELECT * FROM periodos WHERE id=? FOR UPDATE",period);ensure(p!=null,"El periodo no existe.");
            var before=id==null?null:sql.one(c,"SELECT * FROM secciones WHERE id=? FOR UPDATE",id);ensure(id==null||before!=null,"La sección no existe.");
            ensure(sql.one(c,"SELECT id FROM grados WHERE id=?",grade)!=null,"El grado no existe.");
            if(id!=null){ensure(number(before,"periodo_id")==period&&number(before,"grado_id")==grade,"Para otro periodo o grado, cree una sección nueva.");long used=number(sql.one(c,"SELECT COUNT(*) AS cantidad FROM matriculas WHERE seccion_id=? AND estado='ACTIVA'",id),"cantidad");ensure(cap>=used,"La capacidad no puede ser menor que las matrículas activas.");ensure(active||used==0,"No puede desactivar una sección con matrículas activas.");}
            long key;if(id==null)key=sql.insert(c,"INSERT INTO secciones(periodo_id,grado_id,nombre,capacidad,activa) VALUES(?,?,?,?,?)",period,grade,section,cap,active);
            else{key=id;sql.execute(c,"UPDATE secciones SET nombre=?,capacidad=?,activa=? WHERE id=?",section,cap,active,id);}
            audit.register(c,u.id(),id==null?"CREAR":"ACTUALIZAR","secciones",key,before,sql.one(c,"SELECT * FROM secciones WHERE id=?",key));return null;});
    }
    public void requisito(Usuario u,Long id,String name,boolean required,boolean active)throws SQLException{
        admin(u);String title=required(name,100,"Requisito");
        sql.transaction(c->{var before=id==null?null:sql.one(c,"SELECT * FROM tipos_documento WHERE id=? FOR UPDATE",id);ensure(id==null||before!=null,"El requisito no existe.");
            long key;if(id==null)key=sql.insert(c,"INSERT INTO tipos_documento(nombre,obligatorio,activo) VALUES(?,?,?)",title,required,active);
            else{key=id;sql.execute(c,"UPDATE tipos_documento SET nombre=?,obligatorio=?,activo=? WHERE id=?",title,required,active,id);}
            audit.register(c,u.id(),id==null?"CREAR":"ACTUALIZAR","tipos_documento",key,before,sql.one(c,"SELECT * FROM tipos_documento WHERE id=?",key));return null;});
    }
    public void usuario(Usuario u,String username,String name,String role,char[] password)throws SQLException{
        admin(u);String login=required(username,50,"Usuario"),nombre=required(name,100,"Nombre");ensure(login.matches("[A-Za-z0-9._-]{3,50}"),"Use de 3 a 50 letras, números, puntos, guiones o guiones bajos para el usuario.");
        ensure(Set.of("ADMIN","MATRICULA","DIRECTOR").contains(role),"Rol inválido.");String hash;
        try{hash=PasswordHash.hash(password);}catch(IllegalArgumentException e){throw new BusinessException(e.getMessage());}finally{Arrays.fill(password,'\0');}
        sql.transaction(c->{var r=sql.one(c,"SELECT id FROM roles WHERE nombre=?",role);ensure(r!=null,"Falta cargar el catálogo de roles.");long key=sql.insert(c,"INSERT INTO usuarios(rol_id,nombre,username,password_hash) VALUES(?,?,?,?)",number(r,"id"),nombre,login,hash);audit.register(c,u.id(),"CREAR","usuarios",key,null,Map.of("username",login,"nombre",nombre,"rol",role));return null;});
    }
    public void estadoUsuario(Usuario u,long id,boolean active)throws SQLException{
        admin(u);ensure(id!=u.id(),"No puede desactivar su propia cuenta desde esta pantalla.");
        sql.transaction(c->{var before=sql.one(c,"SELECT id,username,activo FROM usuarios WHERE id=? FOR UPDATE",id);ensure(before!=null,"El usuario no existe.");sql.execute(c,"UPDATE usuarios SET activo=? WHERE id=?",active,id);audit.register(c,u.id(),"CAMBIAR_ESTADO","usuarios",id,before,Map.of("activo",active));return null;});
    }
}

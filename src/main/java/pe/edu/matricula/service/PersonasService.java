package pe.edu.matricula.service;
import pe.edu.matricula.dao.*;
import pe.edu.matricula.model.Usuario;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import static pe.edu.matricula.service.Reglas.*;
import static pe.edu.matricula.dao.SqlDAO.*;
public final class PersonasService {
    private final SqlDAO sql=new SqlDAO();private final AuditoriaDAO audit=new AuditoriaDAO();
    private void permiso(Usuario u){ensure(u.getOperador(),"No tiene permiso para editar fichas.");}
    private String document(String type,String value,boolean required){
        String d=required?required(value,20,"Documento"):optional(value,20,"Documento");
        ensure(Set.of("DNI","CE","PASAPORTE","OTRO").contains(type),"Tipo de documento inválido.");
        if(d!=null&&type.equals("DNI"))ensure(d.matches("[0-9]{8}"),"El DNI debe tener ocho dígitos.");return d;
    }
    public long estudiante(Usuario u,Long id,Map<String,String> v,boolean verified,boolean active,boolean confirm)throws SQLException{
        permiso(u);String code=required(v.get("codigo"),30,"Código");String type=required(v.get("tipo_documento"),15,"Tipo de documento");
        String dni=document(type,v.get("documento"),verified),names=required(v.get("nombres"),80,"Nombres"),last=required(v.get("apellidos"),100,"Apellidos"),address=required(v.get("direccion"),200,"Dirección");
        LocalDate birth=date(v.get("fecha_nacimiento"),"Nacimiento");ensure(!birth.isAfter(LocalDate.now(ZoneId.of("America/Lima"))),"La fecha de nacimiento no puede ser futura.");
        return sql.transaction(c->{
            var before=id==null?null:sql.one(c,"SELECT * FROM estudiantes WHERE id=? FOR UPDATE",id);
            ensure(id==null||before!=null,"La ficha no existe.");
            if(id!=null&&(!verified||!active))ensure(number(sql.one(c,"SELECT COUNT(*) AS cantidad FROM matriculas WHERE estudiante_id=? AND estado='ACTIVA'",id),"cantidad")==0,"El estudiante tiene matrículas activas. No puede desactivar su ficha o retirar su verificación.");
            long matches=number(sql.one(c,"SELECT COUNT(*) AS cantidad FROM estudiantes WHERE nombres=? AND apellidos=? AND fecha_nacimiento=? AND id<>?",names,last,java.sql.Date.valueOf(birth),id==null?0:id),"cantidad");
            ensure(matches==0||confirm,"Hay una ficha con nombres, apellidos y nacimiento coincidentes. Revísela o confirme que es otra persona.");
            long key;
            if(id==null)key=sql.insert(c,"INSERT INTO estudiantes(codigo,tipo_documento,documento,nombres,apellidos,fecha_nacimiento,direccion,identidad_verificada,activo) VALUES(?,?,?,?,?,?,?,?,?)",code,type,dni,names,last,java.sql.Date.valueOf(birth),address,verified,active);
            else{key=id;sql.execute(c,"UPDATE estudiantes SET codigo=?,tipo_documento=?,documento=?,nombres=?,apellidos=?,fecha_nacimiento=?,direccion=?,identidad_verificada=?,activo=? WHERE id=?",code,type,dni,names,last,java.sql.Date.valueOf(birth),address,verified,active,id);}
            audit.register(c,u.id(),id==null?"CREAR":"ACTUALIZAR","estudiantes",key,before,sql.one(c,"SELECT * FROM estudiantes WHERE id=?",key));return key;
        });
    }
    public long apoderado(Usuario u,Long id,Map<String,String> v,boolean active)throws SQLException{
        permiso(u);String type=required(v.get("tipo_documento"),15,"Tipo de documento"),dni=document(type,v.get("documento"),true),names=required(v.get("nombres"),80,"Nombres"),last=required(v.get("apellidos"),100,"Apellidos"),phone=required(v.get("telefono"),25,"Teléfono"),email=optional(v.get("email"),120,"Correo");
        ensure(email==null||email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),"Ingrese un correo válido.");
        return sql.transaction(c->{
            var before=id==null?null:sql.one(c,"SELECT * FROM apoderados WHERE id=? FOR UPDATE",id);ensure(id==null||before!=null,"El apoderado no existe.");
            if(id!=null&&!active)ensure(number(sql.one(c,"SELECT COUNT(*) AS cantidad FROM matriculas WHERE apoderado_id=? AND estado='ACTIVA'",id),"cantidad")==0,"El apoderado es responsable de una matrícula activa.");
            long key;if(id==null)key=sql.insert(c,"INSERT INTO apoderados(tipo_documento,documento,nombres,apellidos,telefono,email,activo) VALUES(?,?,?,?,?,?,?)",type,dni,names,last,phone,email,active);
            else{key=id;sql.execute(c,"UPDATE apoderados SET tipo_documento=?,documento=?,nombres=?,apellidos=?,telefono=?,email=?,activo=? WHERE id=?",type,dni,names,last,phone,email,active,id);}
            audit.register(c,u.id(),id==null?"CREAR":"ACTUALIZAR","apoderados",key,before,sql.one(c,"SELECT * FROM apoderados WHERE id=?",key));return key;
        });
    }
    public void vincular(Usuario u,long student,long guardian,String link,boolean principal)throws SQLException{
        permiso(u);String relation=required(link,30,"Vínculo");
        sql.transaction(c->{
            var e=sql.one(c,"SELECT activo FROM estudiantes WHERE id=? FOR UPDATE",student);ensure(e!=null&&flag(e,"activo"),"El estudiante no existe o está inactivo.");
            var a=sql.one(c,"SELECT activo FROM apoderados WHERE id=? FOR UPDATE",guardian);ensure(a!=null&&flag(a,"activo"),"El apoderado no existe o está inactivo.");
            var before=sql.list(c,"SELECT * FROM estudiante_apoderado WHERE estudiante_id=?",student);
            if(principal)sql.execute(c,"UPDATE estudiante_apoderado SET principal=FALSE WHERE estudiante_id=?",student);
            sql.execute(c,"INSERT INTO estudiante_apoderado(estudiante_id,apoderado_id,vinculo,principal) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE vinculo=VALUES(vinculo),principal=VALUES(principal)",student,guardian,relation,principal);
            audit.register(c,u.id(),"VINCULAR_APODERADO","estudiantes",student,before.toString(),sql.list(c,"SELECT * FROM estudiante_apoderado WHERE estudiante_id=?",student).toString());return null;
        });
    }
}

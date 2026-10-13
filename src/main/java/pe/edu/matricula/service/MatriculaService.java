package pe.edu.matricula.service;
import pe.edu.matricula.dao.*;
import pe.edu.matricula.model.Usuario;
import java.sql.*;
import java.util.*;
import static pe.edu.matricula.dao.SqlDAO.*;
import static pe.edu.matricula.service.Reglas.*;

/** Todas las operaciones del mismo periodo toman primero su bloqueo.
 * La matricula, las vacantes derivadas, el movimiento y la auditoria se confirman juntos. */
public final class MatriculaService {
    private final SqlDAO sql=new SqlDAO();
    private final MatriculaDAO dao=new MatriculaDAO();
    private final AuditoriaDAO audit=new AuditoriaDAO();
    private void operator(Usuario u){ensure(u!=null&&u.getOperador(),"No tiene permiso para modificar matrículas.");}
    private void open(Map<String,Object> p){ensure("ABIERTO".equals(p.get("estado")),"El periodo está cerrado o todavía no está abierto.");}

    public long crear(Usuario user,long student,long period,long section,Long guardian,String note)throws SQLException{
        operator(user);
        return sql.transaction(c->{
            open(dao.periodo(c,period));var s=dao.seccion(c,section);
            ensure(number(s,"periodo_id")==period,"La sección no pertenece al periodo seleccionado.");
            var e=sql.one(c,"SELECT * FROM estudiantes WHERE id=? FOR UPDATE",student);
            ensure(e!=null&&flag(e,"activo"),"El estudiante no existe o está inactivo.");
            if(guardian!=null)responsable(c,student,guardian);
            long previous=number(sql.one(c,"SELECT COUNT(*) AS cantidad FROM matriculas WHERE estudiante_id=? AND periodo_id=? AND estado IN ('ACTIVA','PENDIENTE')",student,period),"cantidad");
            ensure(previous==0,"El estudiante ya tiene una matrícula activa o pendiente en este periodo. Revise ese registro.");
            String code="MAT-"+UUID.randomUUID().toString();
            long id=sql.insert(c,"INSERT INTO matriculas(codigo,estudiante_id,apoderado_id,periodo_id,seccion_id,observacion,creado_por) VALUES(?,?,?,?,?,?,?)",code,student,guardian,period,section,optional(note,500,"Observación"),user.id());
            sql.execute(c,"INSERT INTO documentos_matricula(matricula_id,tipo_documento_id) SELECT ?,id FROM tipos_documento WHERE activo=TRUE",id);
            dao.movimiento(c,id,"CREACION",null,"PENDIENTE",null,section,"Registro de solicitud",user.id());
            audit.register(c,user.id(),"CREAR","matriculas",id,null,dao.matricula(c,id,false));
            return id;
        });
    }
    private void responsable(Connection c,long student,long guardian)throws SQLException{
        var a=sql.one(c,"SELECT * FROM apoderados WHERE id=? FOR UPDATE",guardian);
        ensure(a!=null&&flag(a,"activo"),"El apoderado no existe o está inactivo.");
        ensure(sql.one(c,"SELECT estudiante_id FROM estudiante_apoderado WHERE estudiante_id=? AND apoderado_id=?",student,guardian)!=null,"El apoderado no está vinculado con el estudiante.");
    }
    private Map<String,Object> locked(Connection c,long id)throws SQLException{
        var snapshot=dao.matricula(c,id,false);
        open(dao.periodo(c,number(snapshot,"periodo_id")));
        return dao.matricula(c,id,true);
    }
    public void asignarResponsable(Usuario user,long id,long guardian)throws SQLException{
        operator(user);
        sql.transaction(c->{
            var m=locked(c,id);ensure("PENDIENTE".equals(m.get("estado")),"El responsable solo se asigna a una matrícula pendiente.");
            responsable(c,number(m,"estudiante_id"),guardian);
            sql.execute(c,"UPDATE matriculas SET apoderado_id=? WHERE id=?",guardian,id);
            audit.register(c,user.id(),"ASIGNAR_RESPONSABLE","matriculas",id,m,dao.matricula(c,id,false));return null;
        });
    }
    public void documento(Usuario user,long id,long tipo,boolean verified)throws SQLException{
        operator(user);
        sql.transaction(c->{
            var m=locked(c,id);ensure("PENDIENTE".equals(m.get("estado")),"La revisión documental solo se modifica en matrículas pendientes.");
            ensure(sql.one(c,"SELECT id FROM tipos_documento WHERE id=? AND activo=TRUE",tipo)!=null,"El requisito no existe o está inactivo.");
            var before=sql.one(c,"SELECT * FROM documentos_matricula WHERE matricula_id=? AND tipo_documento_id=?",id,tipo);
            sql.execute(c,"INSERT INTO documentos_matricula(matricula_id,tipo_documento_id,verificado,verificado_por,verificado_en) VALUES(?,?,?,?,IF(?,CURRENT_TIMESTAMP,NULL)) ON DUPLICATE KEY UPDATE verificado=VALUES(verificado),verificado_por=VALUES(verificado_por),verificado_en=VALUES(verificado_en)",id,tipo,verified,verified?user.id():null,verified);
            audit.register(c,user.id(),"VERIFICAR_DOCUMENTO","matriculas",id,before,Map.of("tipo_documento_id",tipo,"verificado",verified));return null;
        });
    }
    public void operar(Usuario user,long id,String action,Long destination,String reason)throws SQLException{
        operator(user);
        if(Set.of("RETIRO","ANULACION","TRASLADO_EXTERNO").contains(action))ensure(user.getAdministrador(),"Esta operación requiere el rol administrador.");
        String motive=required(reason,500,"Motivo");
        sql.transaction(c->{
            var m=locked(c,id);String state=m.get("estado").toString();transition(state,action);
            long oldSection=number(m,"seccion_id"),student=number(m,"estudiante_id"),period=number(m,"periodo_id");
            long newSection=action.equals("CAMBIO_SECCION")&&destination!=null?destination:oldSection;
            if(action.equals("CAMBIO_SECCION"))ensure(destination!=null&&newSection!=oldSection,"Seleccione una sección diferente.");
            var sections=new TreeSet<Long>();sections.add(oldSection);sections.add(newSection);
            Map<String,Object> target=null;
            for(long section:sections){var s=dao.seccion(c,section);if(section==newSection)target=s;}
            ensure(number(target,"periodo_id")==period,"La sección de destino pertenece a otro periodo.");
            if(Set.of("ACTIVACION","CAMBIO_SECCION").contains(action)){
                var e=sql.one(c,"SELECT * FROM estudiantes WHERE id=? FOR UPDATE",student);
                ensure(e!=null&&flag(e,"activo")&&flag(e,"identidad_verificada"),"Verifique la identidad y el estado del estudiante antes de activar la matrícula.");
                ensure(m.get("apoderado_id")!=null,"Asigne un responsable válido antes de activar la matrícula.");
                responsable(c,student,number(m,"apoderado_id"));
                capacity(dao.ocupadas(c,newSection),number(target,"capacidad"));
                ensure(sql.one(c,"SELECT id FROM matriculas WHERE estudiante_id=? AND periodo_id=? AND estado='ACTIVA' AND id<>?",student,period,id)==null,"El estudiante ya tiene una matrícula activa en el periodo.");
                var requirements=sql.list(c,"SELECT id FROM tipos_documento WHERE activo=TRUE AND obligatorio=TRUE FOR SHARE");
                for(var r:requirements){var d=sql.one(c,"SELECT verificado FROM documentos_matricula WHERE matricula_id=? AND tipo_documento_id=?",id,number(r,"id"));ensure(d!=null&&flag(d,"verificado"),"Falta verificar uno o más documentos obligatorios.");}
                if(action.equals("CAMBIO_SECCION")){
                    var origin=sql.one(c,"SELECT grado_id FROM secciones WHERE id=?",oldSection);
                    ensure(number(origin,"grado_id")==number(target,"grado_id"),"El cambio de sección debe conservar el mismo grado.");
                }
            }
            String next=switch(action){case "ACTIVACION"->"ACTIVA";case "CAMBIO_SECCION"->state;case "RETIRO"->"RETIRADA";case "ANULACION"->"ANULADA";case "TRASLADO_EXTERNO"->"TRASLADADA";default->throw new BusinessException("Operación desconocida.");};
            sql.execute(c,"UPDATE matriculas SET estado=?,seccion_id=? WHERE id=?",next,newSection,id);
            dao.movimiento(c,id,action,state,next,oldSection,newSection,motive,user.id());
            audit.register(c,user.id(),action,"matriculas",id,m,dao.matricula(c,id,false));return null;
        });
    }
}

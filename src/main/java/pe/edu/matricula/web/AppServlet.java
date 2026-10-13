package pe.edu.matricula.web;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.matricula.dao.*;
import pe.edu.matricula.model.Usuario;
import pe.edu.matricula.service.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.logging.*;
import static pe.edu.matricula.service.Reglas.*;
@WebServlet("/app/*")
public class AppServlet extends HttpServlet {
    private final SqlDAO sql=new SqlDAO();private final MatriculaService matricula=new MatriculaService();
    private final PersonasService personas=new PersonasService();private final ConfiguracionService config=new ConfiguracionService();
    private static final Logger LOG=Logger.getLogger(AppServlet.class.getName());
    private String path(HttpServletRequest r){return r.getPathInfo()==null||r.getPathInfo().equals("/")?"/inicio":r.getPathInfo();}
    private Usuario user(HttpServletRequest r){return (Usuario)r.getSession().getAttribute("usuario");}
    private String v(HttpServletRequest r,String name){return r.getParameter(name);}
    private long key(HttpServletRequest r,String name){return id(v(r,name),name);}
    private Long optionalKey(HttpServletRequest r,String name){String s=v(r,name);return s==null||s.isBlank()?null:id(s,name);}
    private boolean checked(HttpServletRequest r,String name){return "on".equals(v(r,name));}
    private Map<String,String> values(HttpServletRequest r,String... fields){Map<String,String> result=new HashMap<>();for(String f:fields)result.put(f,v(r,f));return result;}
    private void view(HttpServletRequest r,HttpServletResponse s,String page)throws ServletException,IOException{
        if(Set.of("estudiantes.jsp","apoderados.jsp","vinculos.jsp","detalle.jsp","auditoria.jsp").contains(page)){
            Usuario current=user(r);
            try{sql.transaction(c->{new AuditoriaDAO().register(c,current.id(),"CONSULTAR","pantallas",null,null,Map.of("pantalla",page));return null;});}
            catch(SQLException e){LOG.log(Level.SEVERE,"No se pudo registrar la consulta sensible",e);throw new ServletException("No se pudo registrar la consulta.",e);}
        }
        r.setAttribute("page",page);Object flash=r.getSession().getAttribute("flash");if(flash!=null){r.setAttribute("flash",flash);r.getSession().removeAttribute("flash");}
        r.setAttribute("generado",ZonedDateTime.now(ZoneId.of("America/Lima")).toString());r.getRequestDispatcher("/WEB-INF/views/layout.jsp").forward(r,s);
    }
    private void catalogs(HttpServletRequest r)throws SQLException{
        r.setAttribute("periodos",sql.list("SELECT * FROM periodos ORDER BY fecha_inicio DESC"));
        r.setAttribute("secciones",sql.list("SELECT s.*,p.codigo AS periodo,g.nombre AS grado,n.nombre AS nivel,v.disponibles FROM secciones s JOIN periodos p ON p.id=s.periodo_id JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id JOIN vw_vacantes v ON v.seccion_id=s.id ORDER BY p.codigo DESC,n.id,g.id,s.nombre"));
    }
    protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException{
        try{String route=path(r);switch(route){
            case "/inicio" -> {
                r.setAttribute("resumen",sql.one("SELECT (SELECT COUNT(*) FROM estudiantes) AS estudiantes,(SELECT COUNT(*) FROM apoderados) AS apoderados,(SELECT COUNT(*) FROM matriculas WHERE estado='ACTIVA') AS activas,(SELECT COUNT(*) FROM matriculas WHERE estado='PENDIENTE') AS pendientes"));view(r,s,"inicio.jsp");
            }
            case "/estudiantes" -> {
                String q=optional(v(r,"q"),100,"Búsqueda");q=q==null?"":q;r.setAttribute("q",q);
                r.setAttribute("rows",sql.list("SELECT * FROM estudiantes WHERE codigo LIKE ? OR COALESCE(documento,'') LIKE ? OR CONCAT(nombres,' ',apellidos) LIKE ? ORDER BY id DESC LIMIT 500","%"+q+"%","%"+q+"%","%"+q+"%"));
                Long edit=optionalKey(r,"editar");if(edit!=null)r.setAttribute("edit",sql.one("SELECT * FROM estudiantes WHERE id=?",edit));
                view(r,s,"estudiantes.jsp");
            }
            case "/apoderados" -> {
                String q=optional(v(r,"q"),100,"Búsqueda");q=q==null?"":q;r.setAttribute("q",q);
                r.setAttribute("rows",sql.list("SELECT * FROM apoderados WHERE documento LIKE ? OR CONCAT(nombres,' ',apellidos) LIKE ? ORDER BY id DESC LIMIT 500","%"+q+"%","%"+q+"%"));
                Long edit=optionalKey(r,"editar");if(edit!=null)r.setAttribute("edit",sql.one("SELECT * FROM apoderados WHERE id=?",edit));view(r,s,"apoderados.jsp");
            }
            case "/vinculos" -> {
                r.setAttribute("estudiantes",sql.list("SELECT id,codigo,nombres,apellidos FROM estudiantes WHERE activo=TRUE ORDER BY apellidos,nombres"));
                r.setAttribute("apoderados",sql.list("SELECT id,documento,nombres,apellidos FROM apoderados WHERE activo=TRUE ORDER BY apellidos,nombres"));
                r.setAttribute("rows",sql.list("SELECT ea.*,e.codigo,CONCAT(e.apellidos,', ',e.nombres) AS estudiante,CONCAT(a.apellidos,', ',a.nombres) AS apoderado FROM estudiante_apoderado ea JOIN estudiantes e ON e.id=ea.estudiante_id JOIN apoderados a ON a.id=ea.apoderado_id ORDER BY e.apellidos,a.apellidos"));view(r,s,"vinculos.jsp");
            }
            case "/matriculas" -> {
                catalogs(r);r.setAttribute("estudiantes",sql.list("SELECT id,codigo,nombres,apellidos FROM estudiantes WHERE activo=TRUE ORDER BY apellidos,nombres"));
                String q=optional(v(r,"q"),100,"Búsqueda");q=q==null?"":q;r.setAttribute("q",q);
                r.setAttribute("rows",sql.list("SELECT m.id,m.codigo,m.estado,m.creado_en,e.codigo AS codigo_estudiante,CONCAT(e.apellidos,', ',e.nombres) AS estudiante,p.codigo AS periodo,CONCAT(n.nombre,' / ',g.nombre,' / ',s.nombre) AS seccion FROM matriculas m JOIN estudiantes e ON e.id=m.estudiante_id JOIN periodos p ON p.id=m.periodo_id JOIN secciones s ON s.id=m.seccion_id JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE m.codigo LIKE ? OR e.codigo LIKE ? OR CONCAT(e.nombres,' ',e.apellidos) LIKE ? ORDER BY m.id DESC LIMIT 500","%"+q+"%","%"+q+"%","%"+q+"%"));view(r,s,"matriculas.jsp");
            }
            case "/matriculas/detalle" -> {
                long id=key(r,"id");var m=sql.one("SELECT m.*,CONCAT(e.apellidos,', ',e.nombres) AS estudiante,e.codigo AS codigo_estudiante,p.codigo AS periodo,CONCAT(n.nombre,' / ',g.nombre,' / ',s.nombre) AS seccion,a.nombres AS responsable_nombres,a.apellidos AS responsable_apellidos FROM matriculas m JOIN estudiantes e ON e.id=m.estudiante_id JOIN periodos p ON p.id=m.periodo_id JOIN secciones s ON s.id=m.seccion_id JOIN grados g ON g.id=s.grado_id JOIN niveles n ON n.id=g.nivel_id LEFT JOIN apoderados a ON a.id=m.apoderado_id WHERE m.id=?",id);
                ensure(m!=null,"La matrícula no existe.");r.setAttribute("matricula",m);
                r.setAttribute("responsables",sql.list("SELECT a.id,a.nombres,a.apellidos,ea.vinculo FROM estudiante_apoderado ea JOIN apoderados a ON a.id=ea.apoderado_id WHERE ea.estudiante_id=? AND a.activo=TRUE",m.get("estudiante_id")));
                r.setAttribute("documentos",sql.list("SELECT t.id,t.nombre,t.obligatorio,d.verificado,d.verificado_en,u.nombre AS verificador FROM tipos_documento t LEFT JOIN documentos_matricula d ON d.tipo_documento_id=t.id AND d.matricula_id=? LEFT JOIN usuarios u ON u.id=d.verificado_por WHERE t.activo=TRUE ORDER BY t.id",id));
                r.setAttribute("movimientos",sql.list("SELECT mm.*,u.nombre AS usuario,s1.nombre AS origen,s2.nombre AS destino FROM movimientos_matricula mm JOIN usuarios u ON u.id=mm.usuario_id LEFT JOIN secciones s1 ON s1.id=mm.seccion_anterior_id LEFT JOIN secciones s2 ON s2.id=mm.seccion_nueva_id WHERE mm.matricula_id=? ORDER BY mm.id",id));
                r.setAttribute("destinos",sql.list("SELECT s.id,s.nombre,v.disponibles FROM secciones s JOIN secciones actual ON actual.id=? JOIN vw_vacantes v ON v.seccion_id=s.id WHERE s.periodo_id=actual.periodo_id AND s.grado_id=actual.grado_id AND s.id<>actual.id AND s.activa=TRUE",m.get("seccion_id")));view(r,s,"detalle.jsp");
            }
            case "/vacantes","/reportes" -> {
                catalogs(r);Long period=optionalKey(r,"periodo_id");r.setAttribute("periodo_id",period);
                r.setAttribute("rows",sql.list("SELECT p.codigo AS periodo,n.nombre AS nivel,g.nombre AS grado,v.nombre AS seccion,v.capacidad,v.ocupadas,v.disponibles FROM vw_vacantes v JOIN periodos p ON p.id=v.periodo_id JOIN grados g ON g.id=v.grado_id JOIN niveles n ON n.id=g.nivel_id WHERE (? IS NULL OR p.id=?) ORDER BY p.codigo DESC,n.id,g.id,v.nombre",period,period));
                r.setAttribute("estados",sql.list("SELECT p.codigo AS periodo,m.estado,COUNT(*) AS cantidad FROM matriculas m JOIN periodos p ON p.id=m.periodo_id WHERE (? IS NULL OR p.id=?) GROUP BY p.codigo,m.estado ORDER BY p.codigo DESC,m.estado",period,period));view(r,s,"reportes.jsp");
            }
            case "/configuracion" -> {
                catalogs(r);r.setAttribute("grados",sql.list("SELECT g.id,g.nombre,n.nombre AS nivel FROM grados g JOIN niveles n ON n.id=g.nivel_id ORDER BY n.id,g.id"));r.setAttribute("documentos",sql.list("SELECT * FROM tipos_documento ORDER BY id"));view(r,s,"configuracion.jsp");
            }
            case "/usuarios" -> {r.setAttribute("rows",sql.list("SELECT u.id,u.username,u.nombre,u.activo,r.nombre AS rol FROM usuarios u JOIN roles r ON r.id=u.rol_id ORDER BY u.id"));view(r,s,"usuarios.jsp");}
            case "/auditoria" -> {r.setAttribute("rows",sql.list("SELECT a.*,u.username FROM auditoria a JOIN usuarios u ON u.id=a.usuario_id ORDER BY a.id DESC LIMIT 300"));view(r,s,"auditoria.jsp");}
            default -> s.sendError(404,"La página no existe.");
        }}catch(BusinessException e){r.setAttribute("error",e.getMessage());view(r,s,"error.jsp");}
        catch(SQLException e){LOG.log(Level.SEVERE,"Error de consulta",e);s.setStatus(503);r.setAttribute("error","No se pudo leer la base de datos. Revise que MySQL esté iniciado y los scripts estén cargados.");view(r,s,"error.jsp");}
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s)throws IOException{
        String target="/inicio";try{Usuario u=user(r);switch(path(r)){
            case "/estudiantes/guardar" -> {target="/estudiantes";personas.estudiante(u,optionalKey(r,"id"),values(r,"codigo","tipo_documento","documento","nombres","apellidos","fecha_nacimiento","direccion"),checked(r,"identidad_verificada"),checked(r,"activo"),checked(r,"confirmarCoincidencia"));}
            case "/apoderados/guardar" -> {target="/apoderados";personas.apoderado(u,optionalKey(r,"id"),values(r,"tipo_documento","documento","nombres","apellidos","telefono","email"),checked(r,"activo"));}
            case "/vinculos/guardar" -> {target="/vinculos";personas.vincular(u,key(r,"estudiante_id"),key(r,"apoderado_id"),v(r,"vinculo"),checked(r,"principal"));}
            case "/matriculas/crear" -> {target="/matriculas";long id=matricula.crear(u,key(r,"estudiante_id"),key(r,"periodo_id"),key(r,"seccion_id"),null,v(r,"observacion"));target="/matriculas/detalle?id="+id;}
            case "/matriculas/operar" -> {
                long id=key(r,"id");target="/matriculas/detalle?id="+id;String action=required(v(r,"accion"),30,"Operación");
                switch(action){case "RESPONSABLE" -> matricula.asignarResponsable(u,id,key(r,"apoderado_id"));case "DOCUMENTO" -> matricula.documento(u,id,key(r,"tipo_documento_id"),checked(r,"verificado"));default -> matricula.operar(u,id,action,optionalKey(r,"destino_id"),v(r,"motivo"));}
            }
            case "/configuracion/periodo" -> {target="/configuracion";config.periodo(u,optionalKey(r,"id"),v(r,"codigo"),v(r,"fecha_inicio"),v(r,"fecha_fin"),v(r,"estado"));}
            case "/configuracion/seccion" -> {target="/configuracion";config.seccion(u,optionalKey(r,"id"),key(r,"periodo_id"),key(r,"grado_id"),v(r,"nombre"),key(r,"capacidad"),checked(r,"activa"));}
            case "/configuracion/documento" -> {target="/configuracion";config.requisito(u,optionalKey(r,"id"),v(r,"nombre"),checked(r,"obligatorio"),checked(r,"activo"));}
            case "/usuarios/guardar" -> {target="/usuarios";config.usuario(u,v(r,"username"),v(r,"nombre"),v(r,"rol"),(v(r,"password")==null?"":v(r,"password")).toCharArray());}
            case "/usuarios/estado" -> {target="/usuarios";config.estadoUsuario(u,key(r,"id"),checked(r,"activo"));}
            default -> {s.sendError(404,"La operación no existe.");return;}
        }r.getSession().setAttribute("flash","Operación guardada correctamente.");}
        catch(BusinessException e){r.getSession().setAttribute("flash",e.getMessage());}
        catch(SQLException e){LOG.log(Level.SEVERE,"Error al guardar",e);r.getSession().setAttribute("flash",e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"El registro está duplicado o incumple una relación de la base de datos. Revise sus datos.":"No se pudo guardar la operación. No se aplicaron cambios parciales.");}
        s.sendRedirect(r.getContextPath()+"/app"+target);
    }
}

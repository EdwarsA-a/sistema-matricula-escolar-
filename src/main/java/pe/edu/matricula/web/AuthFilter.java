package pe.edu.matricula.web;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import pe.edu.matricula.dao.SqlDAO;
import pe.edu.matricula.model.Usuario;
import pe.edu.matricula.security.Tokens;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
@WebFilter("/*")
public class AuthFilter implements Filter {
    private final SqlDAO sql=new SqlDAO();private static final Logger LOG=Logger.getLogger(AuthFilter.class.getName());
    public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain)throws IOException,ServletException{
        var req=(HttpServletRequest)request;var res=(HttpServletResponse)response;
        req.setCharacterEncoding("UTF-8");res.setCharacterEncoding("UTF-8");
        res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","DENY");
        res.setHeader("Content-Security-Policy","default-src 'self'; style-src 'self'; script-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");
        // Autorizar la ruta interpretada por el contenedor, no su URL codificada.
        String path=req.getServletPath()+(req.getPathInfo()==null?"":req.getPathInfo());
        if(path.startsWith("/assets/")){chain.doFilter(req,res);return;}
        res.setHeader("Cache-Control","no-store");
        HttpSession session=req.getSession();if(session.getAttribute("csrf")==null)session.setAttribute("csrf",Tokens.create());
        if(req.getMethod().equals("POST")&&!Tokens.matches((String)session.getAttribute("csrf"),req.getParameter("csrf"))){res.sendError(403,"La sesión del formulario cambió. Recargue la página.");return;}
        Usuario u=(Usuario)session.getAttribute("usuario");
        if(path.startsWith("/app")){
            if(u==null){res.sendRedirect(req.getContextPath()+"/login");return;}
            try{
                var row=sql.one("SELECT u.id,u.nombre,u.username,r.nombre AS rol FROM usuarios u JOIN roles r ON r.id=u.rol_id WHERE u.id=? AND u.activo=TRUE",u.id());
                if(row==null){session.invalidate();res.sendRedirect(req.getContextPath()+"/login");return;}
                u=new Usuario(SqlDAO.number(row,"id"),row.get("nombre").toString(),row.get("username").toString(),row.get("rol").toString());session.setAttribute("usuario",u);
            }catch(SQLException e){LOG.log(Level.SEVERE,"No se pudo verificar la sesión",e);res.sendError(503,"No se pudo conectar con la base de datos.");return;}
            boolean administrative=path.startsWith("/app/configuracion")||path.startsWith("/app/usuarios")||path.startsWith("/app/auditoria");
            boolean directorAllowed=Set.of("/app","/app/","/app/inicio","/app/reportes","/app/vacantes").contains(path)&&req.getMethod().equals("GET");
            if(administrative&&!u.getAdministrador()||!u.getOperador()&&!directorAllowed){res.sendError(403,"Su rol no tiene acceso a esta operación.");return;}
        }
        chain.doFilter(req,res);
    }
}

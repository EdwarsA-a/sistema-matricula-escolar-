package pe.edu.matricula.web;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import pe.edu.matricula.dao.*;
import pe.edu.matricula.model.Usuario;
import pe.edu.matricula.security.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;
@WebServlet(urlPatterns={"/login","/logout",""})
public class LoginServlet extends HttpServlet {
    private final SqlDAO sql=new SqlDAO();private static final Logger LOG=Logger.getLogger(LoginServlet.class.getName());
    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
        if(req.getSession().getAttribute("usuario")!=null){res.sendRedirect(req.getContextPath()+"/app/inicio");return;}
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req,res);
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{
        if(req.getServletPath().equals("/logout")){req.getSession().invalidate();res.sendRedirect(req.getContextPath()+"/login");return;}
        HttpSession old=req.getSession();long now=System.currentTimeMillis();
        Long blocked=(Long)old.getAttribute("blockedUntil");
        if(blocked!=null&&blocked>now){req.setAttribute("error","Espere un minuto antes de volver a intentar.");doGet(req,res);return;}
        if(blocked!=null){old.removeAttribute("blockedUntil");old.removeAttribute("attempts");}
        String login=req.getParameter("username"),raw=req.getParameter("password");char[] password=(raw==null?"":raw).toCharArray();
        try{
            var row=login==null||login.length()>50?null:sql.one("SELECT u.id,u.nombre,u.username,u.password_hash,r.nombre AS rol FROM usuarios u JOIN roles r ON r.id=u.rol_id WHERE u.username=? AND u.activo=TRUE",login.strip());
            if(row==null||!PasswordHash.verify(password,row.get("password_hash").toString())){
                int attempts=old.getAttribute("attempts")==null?1:(Integer)old.getAttribute("attempts")+1;old.setAttribute("attempts",attempts);if(attempts>=5)old.setAttribute("blockedUntil",now+60000);
                req.setAttribute("error","Usuario o contraseña incorrectos.");doGet(req,res);return;
            }
            Usuario u=new Usuario(SqlDAO.number(row,"id"),row.get("nombre").toString(),row.get("username").toString(),row.get("rol").toString());
            sql.transaction(c->{new AuditoriaDAO().register(c,u.id(),"INICIAR_SESION","usuarios",u.id(),null,Map.of("username",u.username()));return null;});
            req.changeSessionId();old.setAttribute("usuario",u);old.setAttribute("csrf",Tokens.create());old.removeAttribute("attempts");old.setMaxInactiveInterval(1800);
            res.sendRedirect(req.getContextPath()+"/app/inicio");
        }catch(SQLException e){LOG.log(Level.SEVERE,"No se pudo iniciar sesión",e);req.setAttribute("error","No se pudo conectar con la base de datos. Revise la configuración.");doGet(req,res);}
        finally{Arrays.fill(password,'\0');}
    }
}

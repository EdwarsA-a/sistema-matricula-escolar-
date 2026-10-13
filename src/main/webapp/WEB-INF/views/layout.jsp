<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Matrícula escolar</title><link rel="stylesheet" href="<c:url value='/assets/app.css'/>"></head>
<body><aside class="sidebar"><a class="brand" href="<c:url value='/app/inicio'/>"><span class="brand-mark">M</span><span>Matrícula<br><small>Gestión escolar</small></span></a><nav>
<a href="<c:url value='/app/inicio'/>">Inicio</a>
<c:if test="${sessionScope.usuario.operador}"><a href="<c:url value='/app/estudiantes'/>">Estudiantes</a><a href="<c:url value='/app/apoderados'/>">Apoderados</a><a href="<c:url value='/app/vinculos'/>">Vínculos familiares</a><a href="<c:url value='/app/matriculas'/>">Matrículas</a></c:if>
<a href="<c:url value='/app/vacantes'/>">Vacantes y reportes</a>
<c:if test="${sessionScope.usuario.administrador}"><a href="<c:url value='/app/configuracion'/>">Configuración académica</a><a href="<c:url value='/app/usuarios'/>">Usuarios</a><a href="<c:url value='/app/auditoria'/>">Bitácora</a></c:if>
</nav><div class="session"><strong><c:out value="${sessionScope.usuario.nombre}"/></strong><small><c:out value="${sessionScope.usuario.rol}"/></small><form method="post" action="<c:url value='/logout'/>"><input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrf}'/>"><button class="secondary" type="submit">Cerrar sesión</button></form></div></aside>
<main><header class="topbar"><span>Sistema web de matrícula escolar</span><small>Primaria y secundaria</small></header><c:if test="${not empty flash}"><div class="notice" role="status"><c:out value="${flash}"/></div></c:if><jsp:include page="${page}"/></main>
<script src="<c:url value='/assets/app.js'/>"></script></body></html>

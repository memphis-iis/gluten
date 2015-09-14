<%@ tag language="java" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="webFileName" required="true" %>
<%@ attribute name="state" required="true" %>
<%@ attribute name="dispText" required="true" %>

<c:url value="edit" var="tsEditURL">
    <c:param name="state" value="${state}" />
    <c:param name="fn" value="${webFileName}" />
</c:url>

<a title="Open Transcript" class="btn-sm btn-primary" href="${tsEditURL}"
><span class="glyphicon glyphicon-pencil"></span></a>
&nbsp; ${dispText}

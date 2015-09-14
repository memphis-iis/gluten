<%@ tag language="java" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="activeButton" required="false" %>
<%@ attribute name="title" required="true" fragment="true" %>
<%@ attribute name="additional" required="false" fragment="true" %>

<div class="row">
    <div class="col-md-6">
        <div class="page-header">
            <h1><a href="home">Annotator</a>
                <small>
                    Transcript Annotation Tool
                    -
                    <jsp:invoke fragment="title" />            
                </small>
            </h1>
            <jsp:invoke fragment="additional" />
        </div>
    </div>
    <div class="col-md-6">
        <c:if test="${!empty userPhoto}">
            <div class="pull-right">
                <img src="${userPhoto}" class="img-user" />
            </div>
        </c:if>
        <div class="pull-right">
            <c:out value="${userFullName}" /> 
            (<a href="home?do_logout=yes">Logout</a>) 
            <br/>
            <c:out value="${userEmail}" />
        </div>
        
        <c:if test="${isVerifier or isAssigner or isAssessor}">
            <div class="pull-right" style="padding-right: 16px; padding-top: 8px;">
                
                <c:if test="${isAssigner}">
                    <c:set var="cls" value="btn btn-default btn-sm" />
                    <c:if test="${activeButton eq 'assign'}">
                        <c:set var="cls" value="${cls} active btn-primary" />    
                    </c:if>
                    <a href="admin-assign" class="${cls}">Assign</a>
                </c:if>
                
<%--                 <c:if test="${isVerifier}"> --%>
<%--                     <c:set var="cls" value="btn btn-default btn-sm" /> --%>
<%--                     <c:if test="${activeButton eq 'verify'}"> --%>
<%--                         <c:set var="cls" value="${cls} active btn-primary" />     --%>
<%--                     </c:if> --%>
<%--                     <a href="admin-verify" class="${cls}">Verify</a> --%>
<%--                 </c:if> --%>
                
<%--                 <c:if test="${isAssessor}"> --%>
<%--                     <c:set var="cls" value="btn btn-default btn-sm" /> --%>
<%--                     <c:if test="${activeButton eq 'assess'}"> --%>
<%--                         <c:set var="cls" value="${cls} active btn-primary" />     --%>
<%--                     </c:if> --%>
<%--                     <a href="admin-assess" class="${cls}">Assess</a> --%>
<%--                 </c:if> --%>
                
            </div>
        </c:if>
    </div>
</div>
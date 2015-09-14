<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:basepage>
    <jsp:attribute name="pagetitle">Transcript Annotator - ERROR</jsp:attribute>
    
    <jsp:attribute name="scripts">
    </jsp:attribute>

    <jsp:body>
    
    <!-- We don't use the appheader tag to reduce the chance of breaking the error page -->
    <div class="row">
        <div class="col-md-12">
            <div class="page-header">
                <h1>Annotator <small>Transcript Annotation Tool</small></h1>
            </div>
        </div>
    </div>

    <!-- "Real" content -->
    
    <div class="row">
        <div class="col-md-12">
            <h3>An Error Occurred</h3>
            <div class="alert alert-danger"><c:out value="${errorMessage}" /></div>
        </div>
    </div>
    </jsp:body>
</t:basepage>

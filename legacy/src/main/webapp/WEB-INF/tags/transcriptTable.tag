<%@ tag language="java" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@ attribute name="tableid" required="true" %>
<%@ attribute name="rows" required="true" fragment="true" %>

<table id="${tableid}" class="table table-bordered">
    <thead><tr>
        <th>State</th>
        <th>Session</th>
        <th>Class</th>
        <th>Domain</th>
        <th>Area</th>
        <th>SubArea</th>
        <th>ProblemFromLearner</th>
        <th>LastSaved</th>
    </tr></thead>
    
    <jsp:invoke fragment="rows" />
</table>
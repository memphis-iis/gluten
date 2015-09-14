<%@ tag language="java" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="cf" uri="http://iistdc.memphis.edu/commonsfunc" %>

<%@ attribute name="rows" required="true" type="java.util.List<edu.memphis.iis.tdc.annotator.model.TranscriptSession>" %>
<%@ attribute name="state" required="true" %>

<c:forEach items="${rows}" var="ts">
    <tr>
        <td class="nowrap">
            <t:transcriptFileLink dispText="${state}" 
                state="${state}" webFileName="${ts.webFileName}" 
            />
        </td>
        <td class="nowrap">
            <c:out value="${ts.scriptId}" />
            <c:if test="${ts.verify}">
                <span class="label label-danger">
                    <span class="glyphicon glyphicon-check"></span> Verify
                </span>
            </c:if>
            <c:if test="${ts.training}">
                <span class="label label-warning">
                    <span class="glyphicon glyphicon-book"></span> Training
                </span>
            </c:if>
        </td>
 		<td class="nowrap"><c:out value="${ts.classLevel}" /></td>
        <td style="min-width: 180px;"><c:out value="${ts.domain}" /></td>
        <td style="min-width: 180px;"><c:out value="${ts.area}" /></td>
        <td style="min-width: 180px;"><c:out value="${ts.subarea}" /></td>
        <td><c:out value="${cf:abbreviate(ts.problemFromLearner, 50)}" /></td>
        <td style="min-width: 180px;"><c:out value="${ts.lastSavedTime}" /></td>
    </tr>
</c:forEach>
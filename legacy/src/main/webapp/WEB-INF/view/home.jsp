<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:basepage>
    <jsp:attribute name="pagetitle">Transcript Annotator</jsp:attribute>
    
    <jsp:attribute name="scripts">
        <script>
        $(document).ready(function() {
            $(".table").dataTable();

            $("#tabs").tab();
            
            $("#tabs a").click(function (e) {
                e.preventDefault();
                $(this).tab("show");
            });
        });
        </script>
    </jsp:attribute>

    <jsp:body>
    
    <t:appheader>
        <jsp:attribute name="title">Home</jsp:attribute>
    </t:appheader>

    <div class="row">
        <div class="col-md-12">
            <ul id="tabs" class="nav nav-tabs" data-tabs="tabs">
                <li class="active"><a href="#todo" data-toggle="todo">Left To Do</a></li>
                <li><a href="#pending" data-toggle="pending">Pending (Not Started)</a></li>
                <li><a href="#inprogress" data-toggle="inprogess">In Progress</a></li>
                <li><a href="#completed" data-toggle="completed">Completed</a></li>
                <li><a href="#all" data-toggle="all">ALL</a></li>
            </ul>
        </div>
    </div>
    
    <div class="row">
    <div class="col-md-12">
    <br/>    
    <div id="my-tab-content" class="tab-content">
        <div class="tab-pane active" id="todo">
            <t:transcriptTable tableid="transcriptsAvailable">
                <jsp:attribute name="rows">
                    <t:transcriptRows state="Pending" rows="${pendingSessions}" />
                    <t:transcriptRows state="InProgress" rows="${inProgessSessions}" />
                </jsp:attribute>
            </t:transcriptTable>
        </div>
        <div class="tab-pane" id="pending">
            <t:transcriptTable tableid="transcriptsPending">
                <jsp:attribute name="rows">
                    <t:transcriptRows state="Pending" rows="${pendingSessions}" />
                </jsp:attribute>
            </t:transcriptTable>
        </div>
        <div class="tab-pane" id="inprogress">
            <t:transcriptTable tableid="transcriptsInProgress">
                <jsp:attribute name="rows">
                    <t:transcriptRows state="InProgress" rows="${inProgessSessions}" />
                </jsp:attribute>
            </t:transcriptTable>
        </div>
        <div class="tab-pane" id="completed">
            <t:transcriptTable tableid="transcriptsCompleted">
                <jsp:attribute name="rows">
                    <t:transcriptRows state="Completed" rows="${completedSessions}" />
                </jsp:attribute>
            </t:transcriptTable>
        </div>
        <div class="tab-pane" id="all">
            <t:transcriptTable tableid="transcriptsAll">
                <jsp:attribute name="rows">
                    <t:transcriptRows state="Pending" rows="${pendingSessions}" />
                    <t:transcriptRows state="InProgress" rows="${inProgessSessions}" />
                    <t:transcriptRows state="Completed" rows="${completedSessions}" />
                </jsp:attribute>
            </t:transcriptTable>
        </div>
    </div>
    </div>
    </div>
    
    </jsp:body>
</t:basepage>

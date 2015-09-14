<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:basepage>
    <jsp:attribute name="pagetitle">Transcript Verification</jsp:attribute>
    
    <jsp:attribute name="scripts"><script>
        $(function() {
            //We init our data tables separately since they are a little different
            $("#verificationsInProgress").dataTable({
                "aaSorting": [[ 2, "desc" ]]
            });
            
            $("#transcriptsToVerify").dataTable();

            $("#tabs").tab();            
            $("#tabs a").click(function (e) {
                e.preventDefault();
                $(this).tab("show");
            });
            
            $(".cmdStartVerify").click(function(e) {
                e.preventDefault();
                
                var fn = helper.toTrimStr($(this).data("filename"));
                if (fn.length < 1) {
                    console.log("Verification failed - no filename found for button");
                    return;
                }
                
                $("#filename").val(fn);
                $("#startVerificationForm").trigger("submit");
            });
        });
    </script></jsp:attribute>

    <jsp:body>
    
    <t:appheader>
        <jsp:attribute name="title">Verification</jsp:attribute>
        <jsp:attribute name="activeButton">verify</jsp:attribute>
    </t:appheader>
    
    <div class="row">
        <div class="col-md-12">
            <ul id="tabs" class="nav nav-tabs" data-tabs="tabs">
                <li class="active"><a href="#current" data-toggle="current">Verifications In Progress</a></li>
                <li><a href="#possible" data-toggle="possible">Available for Verification</a></li>
            </ul>
        </div>
    </div>

    <div class="row">
    <div class="col-md-12">
    <br />
        <div id="my-tab-content" class="tab-content">
            <div class="tab-pane active" id="current">    
                <table id="verificationsInProgress" class="table table-bordered table-condensed">
                    <thead><tr>
                        <th>Open</th>
                        <th>Filename</th>
                        <th>LastModified</th>
                    </tr></thead>
                    <tbody>
                    <c:forEach items="${currentVerifyList}" var="item" varStatus="status">
                        <tr>
                            <td class="nowrap controlCell">
                                <t:transcriptFileLink dispText="Continue"  
                                    state="${item.state}" webFileName="${item.webFileName}" 
                                />
                            </td>
                            <td><c:out value="${item.fileName}" /></td>
                            <td><c:out value="${item.lastModified}" /></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
            
            <div class="tab-pane" id="possible">
                <table id="transcriptsToVerify" class="table table-bordered table-condensed">
                    <thead><tr>
                        <th>Verify</th>
                        <th>Filename</th>
                        <th>LastModified</th>
                        <th>Individual Transcripts</th>
                    </tr></thead>
                    <tbody>
                    <c:forEach items="${toVerifyList}" var="item" varStatus="status">
                        <tr>
                            <td class="nowrap controlCell">
                                <a title="Begin Transcript Verification" 
                                    class="btn-sm btn-primary cmdStartVerify" 
                                    href="" data-filename="${item.fileName}"
                                ><span class="glyphicon glyphicon-check"></span></a>
                                &nbsp; Verify
                            </td>
                            <td><c:out value="${item.fileName}" /></td>
                            <td><c:out value="${item.maxLastModified}" /></td>
                            <td>
                                <c:forEach items="${item.children}" var="child">
                                    <div>Completed by ${child.user} at ${child.lastModified}</div>
                                </c:forEach>                            
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>    
    </div>
    </div>
    
    <div style="display:none;">
        <form method="post" id="startVerificationForm">
            <input type="hidden" name="filename" id="filename" value="" />
        </form>
    </div>
    </jsp:body>
</t:basepage>

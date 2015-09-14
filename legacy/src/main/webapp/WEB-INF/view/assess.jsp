<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:basepage>
    <jsp:attribute name="pagetitle">Transcript Assessment</jsp:attribute>
    
    <jsp:attribute name="scripts"><script>
        //Set up a file lookup from the table
        var file_lookup = {
            init: function() {
                var rows = $("#availForAssess tbody tr");                
                $.each(rows, function(index, value) {
                    var cmdData = $(value).find("a.cmdStartAssess");
                    if (!cmdData || !cmdData.length || cmdData.length != 1) {
                        return true; //Keep going
                    }
                    
                    var backUser = helper.toTrimStr(cmdData.data("user"));
                    var backState = helper.toTrimStr(cmdData.data("state"));
                    var backFn = helper.toTrimStr(cmdData.data("filename"));
                    var backIndex = helper.toTrimStr(cmdData.data("index"));
                    
                    //Note that we DO in fact re-populate the file lookup
                    file_lookup[backIndex] = {
                        user: backUser,
                        state: backState,
                        filename: backFn
                    };
                });
            }
        };
    
        $(function() {
            file_lookup.init();
            
            $("#cmdFinalAssess").click(function(e){
                e.preventDefault();
                $("#selectBackFile").modal("hide");
                
                var selected = $("#backFileList").val();
                var selBack = null;
                if (selected && helper.toTrimStr(selected).length > 0) {
                	selBack = file_lookup[selected];
                }
                if (selBack) {
                	$("#trainuser").val(selBack.user);
                    $("#trainstate").val(selBack.state);
                }
                
                $("#startAssessmentForm").trigger("submit");
            });
            
            $(".cmdStartAssess").click(function(e) {
                e.preventDefault();                
                var src = $(this);
                
                var user = helper.toTrimStr(src.data("user"));
                var state = helper.toTrimStr(src.data("state"));
                var fn = helper.toTrimStr(src.data("filename"));
                
                if (user.length < 1 || state.length < 1 || fn.length < 1) {
                    console.log("Cannot assess - data missing- " + user + ":" + state + ":" + fn);
                    return;
                }
                
                $("#dlgTitleFileName").html(fn);
                $("#dlgFileName").html(fn);
                $("#dlgUser").html(user);
                $("#dlgState").html(state);
                
                var backDrop = $("#backFileList");
                backDrop.empty();
                backDrop.append($("<option />").val("").text("(No Backing File)"));
                backDrop.val("");
                
                $.each(file_lookup, function(index, value) {
                    var backUser = helper.toTrimStr(value.user);
                    var backState = helper.toTrimStr(value.state);
                    var backFn = helper.toTrimStr(value.filename);
                    
                    if (backFn != fn) {
                        return true; //Filenames must match
                    }
                    else if (backUser == user && backState == backState) {
                        return true; //Don't use the exact same file for backing
                    }
                    
                    var dispText = backUser + " (" + backState + ") " + backFn; 
                    
                    backDrop.append($("<option />").val(index).text(dispText));
                });
                
                //Setup everything the form needs for posting if they decide to go
                $("#user").val(user);
                $("#state").val(state);
                $("#filename").val(fn);
                $("#trainuser").val("");
                $("#trainstate").val("");
                
                $("#selectBackFile").modal("show");
            });
            
            //Do data table setup LAST since we process the rows in the table 
            $("#availForAssess").dataTable({
                "aaSorting": []
            });
        });
    </script></jsp:attribute>

    <jsp:body>
    
    <t:appheader>
        <jsp:attribute name="title">Assessment</jsp:attribute>
        <jsp:attribute name="activeButton">assess</jsp:attribute>
    </t:appheader>

    <div class="row">
        <div class="col-md-12">
            <table id="availForAssess" class="table table-bordered table-condensed">
                <thead><tr>
                    <th>Assess</th>
                    <th>Filename</th>
                    <th>LastModified</th>
                    <th>Status</th>
                    <th>User</th>
                </tr></thead>
                <tbody>
                <c:forEach items="${assessList}" var="item" varStatus="status">
                    <tr>
                        <td class="nowrap controlCell">
                            <a title="Assess Transcript" class="btn-sm btn-primary cmdStartAssess" href=""
                            data-user="${item.user}"
                            data-state="${item.state}"
                            data-filename="${item.fileName}"
                            data-index="${status.index}"
                            ><span class="glyphicon glyphicon-search"></span></a>
                            &nbsp; Assess
                        </td>
                        
                        <td><c:out value="${item.fileName}" /></td>
                        <td><c:out value="${item.lastModified}" /></td>
                        <td><c:out value="${item.state}" /></td>
                        <td><c:out value="${item.user}" /></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
    
    <div id="selectBackFile" class="modal fade"><div class="modal-dialog"><div class="modal-content">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <h4 class="modal-title">Assess File <span id="dlgTitleFileName"></span></h4>
        </div>
        <div class="modal-body">
            You are about to assess<br/> 
            File <span id="dlgFileName" class="label label-primary dlgDataDisp"></span><br/>
            For User <span id="dlgUser" class="label label-primary dlgDataDisp"></span><br/> 
            which is currently in 
            Status <span id="dlgState" class="label label-primary dlgDataDisp"></span><br/>
            <br/>
            Please choose a "backing" file to be used.  (Hint: this is the
            equivalent of choosing the training data for the file you're about
            to examine)
            <br/>
            <select id="backFileList" class="editControl">
                <option value=""></option>
            </select>
            <br /><br />
            <div class="alert alert-info">
            If you do NOT select a "backing" transcript, you'll still be allowed to open
            (assess) the transcript you selected.
            </div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
            <button id="cmdFinalAssess" type="button" class="btn btn-primary">Assess!</button>
        </div>
    </div></div></div>
    
    <div style="display:none;">
        <form method="post" id="startAssessmentForm">
            <input type="hidden" name="user" id="user" value="" />
            <input type="hidden" name="state" id="state" value="" />
            <input type="hidden" name="filename" id="filename" value="" />
            <input type="hidden" name="trainuser" id="trainuser" value="" />
            <input type="hidden" name="trainstate" id="trainstate" value="" />
        </form>
    </div>
    
    </jsp:body>
</t:basepage>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:basepage>
    <jsp:attribute name="pagetitle">Transcript Assignment</jsp:attribute>
    
    <jsp:attribute name="scripts"><script>
        $(function() {
            $("#cmdFinalAssign").click(function(e){
                e.preventDefault();
                
                var targetUser = helper.toTrimStr($("#targetUserList").val());
                var targetState = helper.toTrimStr($("#targetStateList").val());
                
                if (targetUser.length < 1) {
                    alert("You must select a user!");
                    return false;
                }
                else if (targetState.length < 1) {
                    alert("You must select a state!");
                    return false;
                }
                else if (targetUser == $("#user").val() && targetState == $("#state").val()) {
                    alert("You cannot assign a transcript to the same location");
                    return false;
                }
                
                $("#selectBackFile").modal("hide");
                
                $("#targetuser").val(targetUser);
                $("#targetstate").val(targetState);
                
                $.ajax({
                    type: "POST",
                    data: $("#startAssignmentForm").serialize()
                })
                .done(function(data, textStatus, jqXHR){
                    console.log("Assign done done: " + textStatus + ", data:" + data);
                    if (!data || !data.success) {
                        var errMsg = "???";
                        try {
                            errMsg = data.errmsg;
                        } catch(err) {}
                        helper.dispMessage(false, "There was an issue assigning the transcript: " + errMsg);
                    }
                    else {
                        helper.dispMessage(true, "The transcript was successfully copied");
                    }
                })
                .fail(function(jqXHR, textStatus, errorThrown){
                    console.log("ASSIGN FAIL: " + textStatus + ", error:" + errorThrown);
                    helper.dispMessage(false,
                        "There was an error assigning the transcript: " + 
                        "[" + textStatus + ": " + errorThrown + "]"
                    );
                });
            });
            
            $(".cmdStartAssign").click(function(e) {
                e.preventDefault();                
                var src = $(this);
                
                var user = helper.toTrimStr(src.data("user"));
                var state = helper.toTrimStr(src.data("state"));
                var fn = helper.toTrimStr(src.data("filename"));
                
                if (user.length < 1 || state.length < 1 || fn.length < 1) {
                    console.log("Cannot assign - data missing- " + user + ":" + state + ":" + fn);
                    return;
                }
                
                $("#dlgTitleFileName").html(fn);
                $("#dlgFileName").html(fn);
                $("#dlgUser").html(user);
                $("#dlgState").html(state);
                
                //Setup everything the form needs for posting if they decide to go
                $("#user").val(user);
                $("#state").val(state);
                $("#filename").val(fn);
                $("#targetuser").val("");
                $("#targetstate").val("");
                
                $("#selectBackFile").modal("show");
            });
            
            //Do data table setup LAST since we process the rows in the table 
            $("#availForAssign").dataTable({
                "aaSorting": []
            });
        });
    </script></jsp:attribute>

    <jsp:body>
    
    <t:appheader>
        <jsp:attribute name="title">Assignment</jsp:attribute>
        <jsp:attribute name="activeButton">assign</jsp:attribute>
    </t:appheader>

    <div class="row">
        <div class="col-md-12">
            <table id="availForAssign" class="table table-bordered table-condensed">
                <thead><tr>
                    <th>Assign</th>
                    <th>Filename</th>
                    <th>LastModified</th>
                    <th>Status</th>
                    <th>User</th>
                </tr></thead>
                <tbody>
                <c:forEach items="${assignList}" var="item" varStatus="status">
                    <tr>
                        <td class="nowrap controlCell">
                            <a title="Assign Transcript" class="btn-sm btn-primary cmdStartAssign" href=""
                            data-user="${item.user}"
                            data-state="${item.state}"
                            data-filename="${item.fileName}"
                            data-index="${status.index}"
                            ><span class="glyphicon glyphicon-search"></span></a>
                            &nbsp; Assign
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
            <h4 class="modal-title">Assign File <span id="dlgTitleFileName"></span></h4>
        </div>
        <div class="modal-body">
            You are about to assign<br/> 
            File <span id="dlgFileName" class="label label-primary dlgDataDisp"></span><br/>
            For User <span id="dlgUser" class="label label-primary dlgDataDisp"></span><br/> 
            which is currently in 
            Status <span id="dlgState" class="label label-primary dlgDataDisp"></span><br/>
            <br/>
            Please choose the destination.  The file will be <strong>copied</strong>
            to that location
            <br/>
            <select id="targetUserList" class="editControl">
                <option value=""></option>
                <c:forEach items="${assignUsers}" var="item" varStatus="status">
                    <option value="${item}">${item}</option>
                </c:forEach>
            </select>
            <select id="targetStateList" class="editControl">
                <option value=""></option>
                <c:forEach items="${assignStates}" var="item" varStatus="status">
                    <option value="${item}">${item}</option>
                </c:forEach>
            </select>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
            <button id="cmdFinalAssign" type="button" class="btn btn-primary">Assign!</button>
        </div>
    </div></div></div>
    
    <div style="display:none;">
        <form method="post" id="startAssignmentForm">
            <input type="hidden" name="user" id="user" value="" />
            <input type="hidden" name="state" id="state" value="" />
            <input type="hidden" name="filename" id="filename" value="" />
            <input type="hidden" name="targetuser" id="targetuser" value="" />
            <input type="hidden" name="targetstate" id="targetstate" value="" />
        </form>
    </div>
    
    </jsp:body>
</t:basepage>

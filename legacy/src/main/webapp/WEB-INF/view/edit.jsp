<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:basepage>
    <jsp:attribute name="pagetitle">Transcript Annotation</jsp:attribute>
    
    <jsp:attribute name="head">
        <!-- Overridden styles for this page only -->
        <style>
            /* General edit control settings */
            .editControl {
                width: 195px;
            }
            
            /* Make the table header pretty and UoM'ish */
            th {
                background-color: #c8c9cb;
                color: #49679d;
            }
            
            /* Note that editCell is for identification, so no real styling
               is used.  The others are used to specify styling for the
               various cells on a given row */
            td.editCell {
            }
            td.editCellAct {
                white-space: nowrap;
            }
            td.editCellSubAct {
                white-space: nowrap;
            }
            td.editCellMode {
                white-space: nowrap;
            }
            td.editCellComments {
                min-width: 120px;
            }
            td.editCellTagConf {
            }
            
            .tooltip-inner {
            	width: auto;
            	max-width: 600px; 
            }
        </style>
    </jsp:attribute>

    <jsp:attribute name="scripts">
        <!-- Make sure we have the taxonomy available for script -->
        <script src="taxonomy"></script>
        
        <!-- We use a jquery scroll-into-view plugin -->
        <script src="js/jquery.scrollintoview.min.js"></script>
        
       
        
        <script>
        //Sending training data in a hidden table is nice for debugging and code
        //editors, but we would rather have it in native JavaScript.  Rather than
        //having two loops, we just suck in the table data and blow away the table
        var training_mode = {
            on: false,
            type: "",
            
            is: function(s) {
            	return training_mode.on && training_mode.type == s;
            },
            
            items: null,
            
            init: function() {
                //Training mode?  And if so, what type?
                training_mode.on = $("#trainingScript").exists();
                if (training_mode.on) {
                    if ($("#trainingIsVerify").exists()) {
                        training_mode.type = "verify";
                    }
                    else if ($("#trainingIsAssess").exists()) {
                        training_mode.type = "assess";
                    }
                }
                    
                //Handy mode text and styling so they know what mode we're in 
                $("#titleBadge").removeClass("label-info");
                $("#titleBadge").removeClass("label-warning");
                $("#titleBadge").removeClass("label-danger");
                if (!training_mode.on) {
                    training_mode.items = null;
                    $("#titleBadge").html("Standard Tagging").addClass("label-info");
                    return;
                }
                else if (training_mode.type == "verify") {
                    $("#titleBadge").html("Verification Mode").addClass("label-danger");
                }
                else if (training_mode.type == "assess") {
                    $("#titleBadge").html("Assessment Mode").addClass("label-danger");
                }
                else {
                    $("#titleBadge").html("Training Mode").addClass("label-warning");
                }
                
                //We have items
                training_mode.items = {};
                
                $.each($("#trainingScript tbody tr"), function(index, value) {
                    var row = $(value);
                    var rowval = function(cls) {
                        return helper.toTrimStr(row.find(cls).html());   
                    };
                    
                    var idx = row.data("trainindex");
                    
                    var comment_text = Encoder.htmlDecode(rowval("td.trainComments"));
                    
                    training_mode.items[idx] = {
                        act:      rowval("td.trainAct"),
                        subact:   rowval("td.trainSubAct"),
                        mode:     rowval("td.trainMode"),
                        comments: comment_text
                    };
                });
                
                //Now we can kill the table
                $("#trainingScript").remove();
            },
            
            //Return true if there's a training mismatch
            mismatch: function(index, act, subact, mode, tagConf) {
                if (!training_mode.on) {
                    return false; //Not training mode - can be a mismatch
                }
                
                if (training_mode.type == "verify") {
                    return tagConf == "0"; //verify mode - mismatch is based on checkbox
                }
                
                var obj = training_mode.items[index];
                if (!obj || obj == null) {
                    console.log("Missing training object for " + index);
                    return false; //Sucks, but not a mismatch
                }
                
                if (act    != obj.act)    return true;
                if (subact != obj.subact) return true;
                //Don't count mode for training mismatch
                if (mode   != obj.mode)   return true;
                
                return false;
            }
        };
        
        //Colors used for modes - all dialog modes will become a property with
        //the color as value (as a string version of the HMTL RGB triplet). 
        //Also note that our "methods" on this object start with '_'
        var mode_colors = {
            _populate: function(modes) {
                var dialogModes = taxonomy["dialogModes"];
                if (!dialogModes || !dialogModes.length || dialogModes.length < 2) {
                    console.log("No Dialog Modes found in the taxonomy!");
                    return;
                }
                
                var modeCount = dialogModes.length;
                mode_colors["unspecified"] = "#ffffff";
                
                for(var i = 0; i < modeCount; ++i) {
                    var mode = helper.toTrimStr(dialogModes[i]).toLowerCase();
                    if (!mode || mode == "" || mode == "unspecified") {
                        continue;
                    }
                    var hue = i / modeCount;
                    mode_colors[mode] = helper.HSVtoRGB(hue, 0.40, 0.90);
                }
            },
            
            _current: "unspecified",
            _startModeUpdate: function() {
                mode_colors._current = "unspecified";
            },
            
            _updateModeCell: function(modeCell) {
                var mode = helper.toTrimStr(modeCell.html()).toLowerCase();
                if (!mode || mode == "") {
                    mode = "unspecified";
                }
                
                if (mode != "unspecified") {
                    mode_colors._current = mode; //A change!
                }
                
                var color = mode_colors[mode_colors._current];
                modeCell.css("background-color", color);                
            }
        };
        
        //Current editing data with some utility methods
        var currentEdit = {
            currentRow: null,
            act: "",
            subact: "",
            mode: "",
            comments: "",
            confidence: 1,
            
            isEditing: function() {
                return this.currentRow != null;
            },
            
            clear: function() {
                this.currentRow = null;
                this.act = "";
                this.subact = "";
                this.mode= "";
                this.comments = "";
                this.confidence = 1;
            },
            
            prevrow: function() {
                if (this.currentRow == null)
                    return null;
                var r = this.currentRow.prev();
                return r.exists() ? r : null;
            },
            
            nextrow: function() {
                if (this.currentRow == null)
                    return null;
                var r = this.currentRow.next();
                return r.exists() ? r : null;
            },
            
            log: function(t) {
                if (this.currentRow) {
                    console.log(t + " -> " + [this.act, this.subact, this.mode, this.comments, this.confidence].join("|"));
                }
                else {
                    console.log(t + " -> Not currently editing");
                }
            }
        };
        
     
            
        //Autosave state tracking and functionality
        var autosave = {
            dirty: false,
            in_progress: false,
            err_count: 0,
            manual_in_progress: false,
            
            init: function() {
                //When all ajax requests are finished, we know autosave isn't
                //in progress
                $(document).ajaxStop(function(){
                    console.log("ajaxStop fired");
                    autosave.in_progress = false;
                    $(".save-button").prop("disabled", false);
                    //$(".complete-button").prop("disabled", false);
                });
            },
            
            setDirty: function(newDirty) {
                //Here is where we might have additional hooks one day
                autosave.dirty = !!newDirty;
            },
            
            //Wrapper around the helper disp message for failure tracking
            dispMessage: function(success, msgText) {
                if (!success) {
                    if (++autosave.err_count > 10) {
                        msgText += "... NOTE: Auto-saving has been " +
                            "disabled for the remainder of this session due to excessive errors";
                    }
                }
                helper.dispMessage(success, msgText);
            },
            
            checkAutoSave: function() {
                if (autosave.manual_in_progress 
                		|| !autosave.dirty 
                		|| autosave.in_progress 
                		|| autosave.err_count > 10
                		|| training_mode.is("assess"))
                {
                    console.log("autosave skipped");
                    return;
                }
                console.log("autosave COMMENCING!");
                
                //Clear previous messages
                helper.clearDispMessage();
                
                //Get and fix up save data: Send in a simple object form
                //(which will be a posted form)
                var saveData = readSaveData();
                var payload = {
                    autosave: true,
                    fulldata: JSON.stringify(saveData.utts)
                };
                
                //Need to add any fields from save data that we didn't get
                //in the utterances (like soundness)
                for (ky in saveData) {
                	if (ky != "utts") {
                		payload[ky] = saveData[ky];
                	}
                }
                
                //Do save
                autosave.in_progress = true;
                $(".save-button").prop("disabled", true);
                $.ajax({
                    type: "POST",
                    data: payload,
                    dataType: "json"
                })
                .done(function(data, textStatus, jqXHR){
                    console.log("Autosave done: " + textStatus + ", data:" + data);
                    if (!data || !data.success) {
                        var errMsg = "???";
                        try {
                            errMsg = data.errmsg;
                        } catch(err) {}
                        autosave.dispMessage(false, "There was an issue auto-saving your data: " + errMsg);
                    }
                    else {
                        autosave.setDirty(false);
                        if (autosave.err_count > 1) {
                            autosave.err_count = Math.ceil(autosave.err_count / 2);
                        }
                        autosave.dispMessage(true, "Your annotation was autosaved.");
                    }
                })
                .fail(function(jqXHR, textStatus, errorThrown){
                    console.log("Autosave FAIL: " + textStatus + ", error:" + errorThrown);
                    autosave.dispMessage(false,
                        "There was an error auto-saving your data: " + 
                        "[" + textStatus + ": " + errorThrown + "]"
                    );
                });
            }
        };
        
        //Return true if the given data would mark a row as tagged
        //NOTE that act and subact MUST be strings and not null
        //(You can use helper.toTrimStr to ensure this if required)
        function isTagged(act, subact) {
            if (act.length > 1) {
                //We allow "Unspecified" to be a correct tag for a row,
                //but in that case there will be no subact
                return subact.length > 1 || act.toLowerCase() == "unspecified";
            }
            else {
                //Must have an act tag
                return false;
            }
        }
        
        //Perform any row-specified (per-utterance) styling
        function reStyleRows() {
            var rows = $("#transcriptUtterances tbody tr");
            
            //Need to init dialog mode coloring
            mode_colors._startModeUpdate();
            
            $.each(rows, function(index, value) {
                var editCells = $(value).find("td.editCell");
                if (!editCells || !editCells.length || editCells.length != 5) {
                    return true; //Keep going
                }
                
                var itemIndex = $(value).data("utteranceindex");
                
                var act = helper.toTrimStr($(editCells[0]).html());
                var subact = helper.toTrimStr($(editCells[1]).html());
                var modeCell = $(editCells[2]);
                var mode = helper.toTrimStr(modeCell.html());
                var tagConf = $(editCells[4]).data("confidence");
                
                //We style the row buttons based on tag state
                var button = $(value).find("button.startTagBtn");
                if (button.exists()) {
                    button.removeClass("btn-info");
                    button.removeClass("btn-danger");
                    button.removeClass("btn-warning");
                    button.removeClass("btn-success");
                    
                    if (!isTagged(act, subact)) {
                        button.addClass("btn-warning");
                    }
                    else if (training_mode.mismatch(itemIndex, act, subact, mode, tagConf)) {
                        button.addClass("btn-danger");
                    }
                    else {
                        button.addClass("btn-success");
                    }
                }
                
                //Update mode colors
                mode_colors._updateModeCell(modeCell);
            });
        }
        
        //Handle the editor, clean out the current row, and re-add the given values
        function reAddEditVals(act, subact, mode, comments, confidence) {
            //Clear any tooltips and popovers
            $(".needTooltip").tooltip("hide");
            $(".needPopover").popover("hide");
            
            //Hide our editor
            var dlg = $("#taggingDlg").detach();
            $("#taggingDlgContainer").append(dlg);
            
            //Find and clear the row
            var row = $(currentEdit["currentRow"]);
            row.find("td.editCell").remove();
            row.find("td.editor").remove();
            
            //Confidence gets some pretty special formatting.  Also - it's
            //either 0 or we assume "perfect confidence"
            var conf_html = "";
            var conf_data = "";
            if (confidence != "0") {
                conf_html = "";
                conf_data = "1";
            }
            else {
                conf_html = "<span class='label label-danger'><strong>?</strong></span>";
                conf_data = "0";
            }
            
            
            //Add data back in
            var addCell = function(cls, val) {
                var ret = $("<td></td>").addClass("editCell").addClass(cls).html(val);
                row.append(ret);
                return ret;
            };
            addCell("editCellAct", act);
            addCell("editCellSubAct", subact);
            addCell("editCellMode", mode);
            addCell("editCellComments", Encoder.htmlEncode(comments));
            addCell("editCellTagConf", conf_html).data("confidence", conf_data);
        }
        
        //If necessary, save the current edit and put everything back to non-edit mode 
        function finishEdit() {
            if (!currentEdit.isEditing()) {
                //Nothing to do
                return;
            }
            
            //In assessment mode, no one can hear you save
            if (training_mode.is("assess")) {
            	cancelEdit();
            	return;
            }
            
            currentEdit.log("Saving");
            
            //read editor control values
            var act = $("#editDlgAct").val();
            var subact = $("#editDlgSubAct").val();
            var mode = $("#editDlgMode").val();
            var comments = $("#editDlgComments").val();
            var confidence = $("#editDlgTagConf").prop("checked") ? "0" : "1";
            
            //Now we can add fresh editCell cells and reset
            reAddEditVals(act, subact, mode, comments, confidence);
            currentEdit.clear();
            reStyleRows();
            
            //On change, we assume data is dirty
            autosave.setDirty(true);
            autosave.checkAutoSave();
        }
        
        function cancelEdit() {
            var ce = currentEdit; //Just for short code
            
            if (!ce.isEditing()) {
                //Nothing to do
                return;
            }
            
            //Just reset vals saved when edit started and reset            
            reAddEditVals(ce.act, ce.subact, ce.mode, ce.comments, ce.confidence);
            ce.clear();
            reStyleRows();
        }
        
        function startEdit(editRow) {
            finishEdit();
            
            if (!editRow) {
                return;
            }
            
            var editCells = null;
            if (editRow) {
                editCells = editRow.find("td.editCell");
            }
            
            if (!editCells || !editCells.length || editCells.length != 5) {
                console.log("Invalid editRow/editCell: " + editRow + ", " + editCells);
                console.log(editRow);
                console.log(editCells);
                alert("Something is wrong with the editing system - you may want to save and then refresh the screen");
                return;
            }
            
            var editIdx = editRow.data("utteranceindex");
            
            //Save our editing data
            currentEdit["currentRow"] = editRow;
            currentEdit["act"]        = $(editCells[0]).html();
            currentEdit["subact"]     = $(editCells[1]).html();
            currentEdit["mode"]       = $(editCells[2]).html();
            currentEdit["comments"]   = Encoder.htmlDecode($(editCells[3]).html());
            currentEdit["confidence"] = $(editCells[4]).data("confidence");
            
            //Clear any previous training display
            $("#editDlgTrainingDisplay").html("").hide();
            //...and set up for display if necessary
            try {
                if (training_mode.on) {
                    var train = training_mode.items[editIdx];
                    
                    var commentText = Encoder.htmlEncode(train.comments);
                    if (training_mode.type == "verify") {
                        commentText = "Comments<br /> &raquo; " + 
                            commentText.replace(/\{end\}/g, " <br />&raquo; ");
                        
                        commentText = helper.stripSuffix(commentText, "&raquo; ");
                    }
                    else {
                        commentText = "Comments: " + commentText;
                    }
                    $("#editDlgTrainingDisplay").html(
                            "Act: " + train.act + ", " +
                            "SubAct: " + train.subact + ", " +
                            "Mode: " + train.mode + "<br />" +
                            commentText);
                }
            }
            catch(e) {
                console.log(e);
            }
            
            //Remove the edit cells and add our merged editor cell
            editCells.remove();
            var editor = $("<td colspan='5'></td>").addClass("editor"); 
            editRow.append(editor);

            //Populate editor controls 
            $("#editDlgAct").val(currentEdit["act"]);
            populateSubAct(); //Force subact drop down re-sync
            $("#editDlgSubAct").val(currentEdit["subact"]);
            $("#editDlgMode").val(currentEdit["mode"]);
            $("#editDlgComments").val(currentEdit["comments"]);
            $("#editDlgTagConf").prop("checked", currentEdit["confidence"] == "0");
            
            //Init/Show our editor
            var dlg = $("#taggingDlg").detach();
            editor.append(dlg);
            $("#dialogEditorPrevRow").prop("disabled", currentEdit.prevrow() == null);
            $("#dialogEditorNextRow").prop("disabled", currentEdit.nextrow() == null);
            
            //Make sure they can see the editor - note they might also want
            //to see the next row for context
            $("#editDlgAct").scrollintoview();
            $("#editDlgFooter").scrollintoview();
            var nextRow = currentEdit.nextrow();
            if (nextRow != null)
                nextRow.scrollintoview();            
            
            currentEdit.log("Edit Started");
            
            //Let touch screen devices handle their own focus for this
            if (!helper.isTouchBrowser()) {
                $("#editDlgAct").focus();
            }
        }
        
        function editNext() {
            if (!currentEdit.isEditing())
                return;
            
            var next = currentEdit.nextrow();
            finishEdit();
            
            if (next != null) {
                startEdit(next);
            }
        }
        
        function editPrev() {
            if (!currentEdit.isEditing())
                return;
            
            var prev = currentEdit.prevrow();
            finishEdit();
            
            if (prev != null) {
                startEdit(prev);
                //startEdit handles "normal" scrolling down, but we might
                //need to scroll up
                $("#editDlgHeader").scrollintoview();
            }
        }
        
        function populateSubAct() {
            var selCombo = $("#editDlgAct");
            var sel = helper.toTrimStr(selCombo.val());
            
            var subselCombo = $("#editDlgSubAct");
            var subsel = helper.toTrimStr(subselCombo.val());
            subselCombo.empty();
            subselCombo.append($("<option />").val("").text(""));
            subselCombo.val("");
            
            if (sel == "" || sel.toLowerCase() == "unspecified") {
                //Done - leave the sub type blank
                return;
            }
            
            if (!taxonomy) {
                alert("No Tagging taxonomy found!");
                return;
            }
            var dialogAct = taxonomy["dialogActs"][sel];
            if (!dialogAct || !dialogAct.subtypes) {
                console.log("No Dialog Act object found in the taxonomy for " + sel);
                console.log(taxonomy);
                console.log(taxonomy["dialogActs"]);
                console.log(taxonomy["dialogActs"][sel]);
                return;
            }
            
            var subtypes = dialogAct.subtypes;  
            if (!subtypes.length || subtypes.length < 1) {
                console.log("No subtypes found in Dialog Act object taxonomy for " + sel);
                return;
            }
            
            var firstVal = "";
            var realLen = 0;
            for(var i = 0; i < subtypes.length; ++i) {
                var st = subtypes[i];
                if (helper.toTrimStr(st).length > 0) {
                    realLen += 1;
                    if (firstVal.length < 1 && st != "Unspecified") {
                        firstVal = st;
                    }
                }
                subselCombo.append($("<option />").val(st).text(st));
            }
            
            //If we have only one value (and it's something "good"), then
            //default to that selection
            if (realLen == 1 && firstVal.length > 0) {
                subsel = firstVal;
            }
            
            //If they already had something selected previously, try and use it
            subselCombo.val(subsel);
        }
        
        function jumpToSoundness(after_scroll) {
            $("#sessionSoundness").parents("table").scrollintoview({
                complete: function() {
                    $("#sessionSoundness").parents("td").effect("highlight", {}, 2000);
                    if (after_scroll) {
                        after_scroll();
                    }
                }
            });
        }
        
        //Read and return data for save
        function readSaveData() {
            var saveData = {
                soundness: helper.toTrimStr($("#sessionSoundness").val()),
                learningAssessmentScore: helper.toTrimStr($("#learningAssessmentScore").val()),
                learningAssessmentComments: helper.toTrimStr($("#learningAssessmentComments").val()),
                sessionComments: helper.toTrimStr($("#sessionComments").val()),
                utts: []
            };       
            
            $(".transcriptRow").each(function(index, value) {
                var checkIndex = helper.safeParseInt($(value).data("utteranceindex"), -1);
                if (checkIndex != index) {
                    console.log("Error saving - rows out of order? " + index + "!=" + checkIndex);
                }
                
                var editCells = $(value).find(".editCell");
                if (!editCells || !editCells.length || editCells.length != 5) {
                    console.log("Error saving - edit cells are wrong? ...");
                    console.log(editCells);
                }
                
                saveData.utts.push({
                    act:        $(editCells[0]).html(),
                    subact:     $(editCells[1]).html(),
                    mode:       $(editCells[2]).html(),
                    comments:   Encoder.htmlDecode($(editCells[3]).html()),
                    confidence: "" + $(editCells[4]).data("confidence"),
                    index:      index
                });
            });
            
            return saveData;
        }
        
        //Initial set up when document is ready
        $(function() {
            //Do any page-wide init we need
            training_mode.init();
            mode_colors._populate();
            reStyleRows();
            
            //Set top-level data fields
            $("#sessionSoundness").val($("#sessionSoundness").data("initval"));
            $("#learningAssessmentScore").val($("#learningAssessmentScore").data("initval"));
            
            //Turn on any tooltips and popovers if we're NOT on a touch device
            if (helper.isTouchBrowser()) {
                //Touch screen - kill all the tooltip-related stuff
                $(".needPopover")
                    .removeClass(".needPopover")
                    .data("toggle", "")
                    .prop("title", "");
                $(".needTooltip")
                    .removeClass(".needTooltip")
                    .data("toggle", "")
                    .prop("title", "");
            }
            else {
                //Setup tooltips and popovers
                $(".needTooltip").tooltip();
                $(".needPopover").popover({trigger:"hover"});
            }
            
            //If we're in assessment mode, remove save-related controls
            if (training_mode.is("assess")) {
            	$("#dialogEditorSave").remove();
            	$(".save-button").remove();
            	$(".complete-button").remove();
            }
            
            //Turn off any "demo" buttons just used for display
            $(".demobtn").click(function(e){
                e.preventDefault();
            });
            
            //Format the "click to reveal" stuff
            helper.setupOptHidden();
                        
            $(".startTagBtn").click(function(e) {
                e.preventDefault();
                startEdit($(this).closest("tr"));
            });
            
            $("#dialogEditorPrevRow").click(function(e) {
                e.preventDefault();
                editPrev();
            });
            
            $("#dialogEditorNextRow").click(function(e) {
                e.preventDefault();
                editNext();
            });
            
            $("#dialogEditorTrainReveal").click(function(e) {
                e.preventDefault();
                $("#editDlgTrainingDisplay").show("slow");
            });
            
            $("#dialogEditorSave").click(function(e) {
                e.preventDefault();
                finishEdit();
            });
            
            $("#dialogEditorCancel").click(function(e) {
                e.preventDefault();
                cancelEdit();
            });
            
            $("#editDlgAct").change(function() {
                populateSubAct();
            });
            
            $("#dialogHelpRequest").keydown(function () {
                $("#editDlgAct").focus();
            });
            
            $("#cmdScrollToSoundness").click(function(e){
                e.preventDefault();
                jumpToSoundness();
            });
            
            var isMappedKey = function(e) {
                //Since we are called for all keydown events, we try to
                //bail as fast as possible
                if (!e.altKey) {
                    return false;
                }
                var kc = e.keyCode;
                if (kc != 78 && kc != 80) { //N=78, P==80
                    return false;
                }
                
                return currentEdit.isEditing();
            };
            
            //Handle our prev/next record keyboard shortcut
            $(document).keydown(function(e) {
                if (isMappedKey(e)) {
                    e.preventDefault();
                    var kc = e.keyCode; //N=78, P==80
                    try {
                        if      (kc == 80) editPrev();
                        else if (kc == 78) editNext();
                    }
                    catch(e) {
                        //Nothing
                    }
                }
            });
            $("form button").click(function() {
                $("button", $(this).parents("form")).removeAttr("clicked");
                $(this).attr("clicked", "true");
            });
            $("#fullSaveForm").submit(function(e){                
                try {
                	//No saving in assess mode
                	if (training_mode.is("assess")) {
                		e.preventDefault();
                        return false;
                	}
                	
                    //First, make sure autosave knows that we're working
                    //Will reset it to false in our finally block
                    autosave.manual_in_progress = true;
                    
                    //Finish any current edit
                    finishEdit();
                    
                    if (autosave.in_progress) {
                        e.preventDefault();
                        alert("There is an autosave in progress, please wait and try again");
                        return false;
                    }
                    
                    var saveData = readSaveData();
                    
                    if (saveData.soundness.length < 1) {
                        e.preventDefault();
                        jumpToSoundness(function(){
                            alert("Please rate this session for educational soundness");
                        });
                        return false;
                    }
                    
                    //Now give them the full form but updated with our gathered data
                    var thisForm = $(this);
                    
                    var addToForm = function(name, data) {
                        thisForm.find("#" + name).remove();
                        
                        thisForm.append($("<input>").attr({
                            type: "hidden",
                            id: name,
                            name: name,
                            value: data
                        }));
                    };
                    addToForm("fulldata", JSON.stringify(saveData.utts));
                    
                    //Add all save data that isn't in the utts (like soundness)
                    for (ky in saveData) {
                    	if (ky != "utts") {
                    		addToForm(ky, saveData[ky]);
                    	}
                    }
                    
                    //If they clicked the completed button, send that also
                    if($("button[clicked=true]").attr("id") == "completed"){
                    	if (confirm("This will move the transcript to the completed folder.\n You will not be able to read or modify your annotation afterwards. \n Do you want to proceed with this action?")){ 
                    		addToForm("completed","true");
                    	}
                    }
                    //Data is now clean
                    autosave.setDirty(false);
                }
                catch(err) {
                    e.preventDefault(); //No form submit if there's an error!
                    console.log("About to show Save Failure Message: " + err);
                    alert("There was an error attempting to save! The message was: " + err);
                }
                finally {
                    autosave.manual_in_progress = false;
                }
            });
            
            //Last action: crank up the autosave checking
            autosave.init();
        });
        </script>
    </jsp:attribute>



    <jsp:body>
    
    <t:appheader>
        <jsp:attribute name="title">
            Edit <span id="titleBadge" class="label"></span>
        </jsp:attribute>
        
        <jsp:attribute name="additional">
            <a href="home">Return to Session Selection</a>
        </jsp:attribute>
    </t:appheader>

    <!-- Transcript meta data -->
    
    <div class="row">
        <div class="col-md-4">
            <table class="table table-bordered">
                <tr><th>Session</th><td><c:out value="${transcript.scriptId}" /></td></tr>
                <tr><th>Begin</th><td><c:out value="${transcript.beginDateTime}" /></td></tr>
                <tr><th>Duration</th><td><c:out value="${transcript.scriptDuration}" /></td></tr>
                <tr><th>Student Lag</th><td><c:out value="${transcript.learnerLagDuration}" /></td></tr>
            </table>
        </div>
        <div class="col-md-8">
            <table class="table table-bordered">
                <tr><th>Domain</th><td><c:out value="${transcript.domain}" /></td></tr>
                <tr><th>Area</th><td><c:out value="${transcript.area}" /></td></tr>
                <tr><th>Subarea</th><td><c:out value="${transcript.subarea}" /></td></tr>
                <tr><th>ProblemFromLearner</th><td><c:out value="${transcript.problemFromLearner}" /></td></tr>
            </table>
        </div>
    </div>
    
    <form method="post" id="fullSaveForm">
    
    <div class="row">
        <div class="col-md-12">
            <table class="table table-bordered table-condensed">
                <tr>
                    <th width="1%" class="nowrap">Learner Notes</th>
                    <td><c:out value="${transcript.learnerNotes}" /></td>
                </tr>
                <tr>
                    <th width="1%" class="nowrap">Tutor Notes</th>
                    <td><c:out value="${transcript.tutorNotes}" /></td>
                </tr>
                <tr>
                    <th width="1%" class="nowrap">Educational Soundness</th>
                    <td>
                        <span class="text-info">
                        This is an educationally sound session:
                        </span>
                        <select id="sessionSoundness" class="editControl" data-initval="${transcript.soundness}">
                            <option value=""></option>
                            <option value="1">Completely Agree</option>
                            <option value="2">Agree</option>
                            <option value="3">Not Sure</option>
                            <option value="4">Disagree</option>
                            <option value="5">Completely Disagree</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <th width="1%" class="nowrap">Learning Assessment</th>
                    <td>
                        <span class="text-info">
                        There is <em>solid evidence</em> that the student acquired important new understanding
                        during the course of this session:
                        </span>
                        <br/>
                        <select style="width:auto;" id="learningAssessmentScore" class="editControl" data-initval="${transcript.learningAssessmentScore}">
                            <option value=""></option>
                            <option value="1">Strong evidence that the student did NOT acquire new understanding</option>
                            <option value="2">Some evidence that the student did NOT acquire new understanding</option>
                            <option value="3">No evidence either way</option>
                            <option value="4">Some evidence of new understanding</option>
                            <option value="5">Strong evidence of new understanding</option>
                        </select>
                        <br/>
                        
                        <span class="text-info">
                        Please explain your score as BRIEFLY as possible..
                        </span>
                        <br/>
                        <textarea name="learningAssessmentComments" id="learningAssessmentComments" 
                            rows="3" cols="64" ><%--  ${transcript.learningAssessmentComments} --%></textarea>
                    </td>
                </tr>
                <tr>
                    <th width="1%" class="nowrap">Overall comments</th>
                    <td>
                        <span class="text-info">
                        General observations about this session
                        </span>
                        <br/>
                        <textarea name="sessionComments" id="sessionComments" 
                            rows="3" cols="64" >${transcript.sessionComments}</textarea>
                    </td>
                </tr>
            </table>
        </div>
    </div>
    
    <br/>
    
    <div class="row">
        <div class="col-md-6">
            <table><tr>
                <td class="nowrap">
                    <button class="btn btn-primary save-button">
                        <span class="glyphicon glyphicon-save"></span> Save Me!
                    </button>
                    <button class="btn btn-primary save-button" id="completed">
                    	<span class="glyphicon glyphicon-save"></span> Completed!
                	</button>
                </td>
                <td class="nowrap" style="padding-left: 12px;">
					<c:choose>
						<c:when test="${empty prevFile}">
							<a class="btn btn-primary disabled" disabled="disabled" role="button" href="#">
								<span class="glyphicon glyphicon-step-backward"></span>Prev File
							</a>
						</c:when>
						<c:otherwise>
							<c:url value="edit" var="prevFileURL">
							    <c:param name="state" value="${prevFile.state}" />
							    <c:param name="fn" value="${prevFile.webFileName}" />
							</c:url>
							<a class="btn btn-primary needTooltip" data-toggle="tooltip" 
								title="${prevFile.state}/${prevFile.fileName}"
								href="${prevFileURL}">
								<span class="glyphicon glyphicon-step-backward"></span>Prev File
							</a>
						</c:otherwise>
					</c:choose>
				</td>
				<td class="nowrap" style="padding-left: 2px;">
					<c:choose>
						<c:when test="${empty nextFile}">
							<a class="btn btn-primary disabled" disabled="disabled" href="#">
								<span class="glyphicon glyphicon-step-forward"></span>Next File
							</a>
						</c:when>
						<c:otherwise>
							<c:url value="edit" var="nextFileURL">
							    <c:param name="state" value="${nextFile.state}" />
							    <c:param name="fn" value="${nextFile.webFileName}" />
							</c:url>
							<a class="btn btn-primary needTooltip" data-toggle="tooltip" 
								title="${nextFile.state}/${nextFile.fileName}"
								href="${nextFileURL}">
								<span class="glyphicon glyphicon-step-forward"></span>Next File
							</a>
						</c:otherwise>
					</c:choose>
                </td>
            </tr></table>
        </div>
        <div class="col-md-1"></div>
        <div class="col-md-5">
            To see details about the Tag button on each row:
            <div class="opthide">
                <p>
                    <button class="btn-xs btn-success demobtn">
                        <span class="glyphicon glyphicon-edit"></span>
                    </button>
                    Row is annotated
                </p>
                <p>
                    <button class="btn-xs btn-warning demobtn">
                        <span class="glyphicon glyphicon-edit"></span>
                    </button>
                    Row still needs to be annotated
                </p>
                <c:if test="${trainingMode}">
                <p>
                    <button class="btn-xs btn-danger demobtn">
                        <span class="glyphicon glyphicon-edit"></span>
                    </button>
                    <em>Training Mode:</em>
                    Your annotation doesn't match the training data
                </p>
                </c:if>
                <c:if test="${verifyMode}">
                <p>
                    <button class="btn-xs btn-danger demobtn">
                        <span class="glyphicon glyphicon-edit"></span>
                    </button>
                    <em>Verify Mode:</em>
                    You need to verify the tagging and uncheck the Not Sure box
                </p>
                </c:if>
                <c:if test="${assessMode}">
                <p>
                    <button class="btn-xs btn-danger demobtn">
                        <span class="glyphicon glyphicon-edit"></span>
                    </button>
                    <em>Assess Mode:</em>
                    View-Only Assessment Mode: the data between the transcript and
                    the "backing" transcript doesn't match
                </p>
                </c:if>
            </div>
        </div>
    </div>
    
    <br/>
    
    <div class="row">
        <div class="col-md-12">        
            <table id="transcriptUtterances" class="table table-bordered table-condensed">
                <thead><tr>
                    <th>Timestamp</th>
                    <th>Speaker</th>
                    <th>Utterance</th>
                    <th>
                        <span class="text-info needTooltip" data-toggle="tooltip" title="Click the button to tag the row">Tag</span>
                    </th>
                    <th>Dialog Act</th>
                    <th>Subtype</th>
                    <th>Dialog Mode</th>
                    <th>Comments</th>
                    <th>?</th>
                </tr></thead>
                <tbody>
                <c:forEach items="${transcript.transcriptItems}" var="item" varStatus="status">
                    <tr class="transcriptRow" data-utteranceindex="${status.index}">
                        <!-- Read Only Fields -->
                        <td><c:out value="${item.timestamp}" /></td>
                        <td><c:out value="${item.dispSpeaker}" /></td>
                        <td><c:out value="${item.text}" /></td>
                        
                        <!-- Editing! -->
                        <td class="nowrap"><button class="btn-xs btn-info startTagBtn">
                            <span class="glyphicon glyphicon-edit"></span>
                        </button></td>
                        <td class="editCell editCellAct"><c:out value="${item.dialogAct}" /></td>
                        <td class="editCell editCellSubAct"><c:out value="${item.dialogSubAct}" /></td>
                        <td class="editCell editCellMode"><c:out value="${item.dialogMode}" /></td>
                        <td class="editCell editCellComments"><c:out value="${item.comments}" /></td>
                        <td class="editCell editCellTagConf" data-confidence="${item.tagConfidence}">
                            <c:choose>
                                <c:when test="${item.tagConfidence eq 0}">
                                    <span class='label label-danger'><strong>?</strong></span>                                    
                                </c:when>
                                <c:otherwise></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
    
    <div class="row">
        <div class="col-md-12">
	        <table><tr>
	        <td class="nowrap">
	            <button class="btn btn-primary save-button">
	                <span class="glyphicon glyphicon-save"></span> Save Me!
	            </button>
	            <button class="btn btn-primary save-button"
						id="completed">
	                <span class="glyphicon glyphicon-save"></span> Completed!
	            </button>
	        </td>
	            
			<td class="nowrap" style="padding-left: 12px;">
				<c:choose>
				<c:when test="${empty prevFile}">
					<a class="btn btn-primary disabled" disabled="disabled" role="button" href="#">
						<span class="glyphicon glyphicon-step-backward"></span>Prev File
					</a>
				</c:when>
				<c:otherwise>
					<c:url value="edit" var="prevFileURL">
					    <c:param name="state" value="${prevFile.state}" />
					    <c:param name="fn" value="${prevFile.webFileName}" />
					</c:url>
					<a class="btn btn-primary needTooltip" data-toggle="tooltip"
						title="${prevFile.state}/${prevFile.fileName}"
						href="${prevFileURL}">
						<span class="glyphicon glyphicon-step-backward"></span>Prev File
					</a>
				</c:otherwise>
			</c:choose>
			</td>
			<td class="nowrap" style="padding-left: 2px;">
				<c:choose>
				<c:when test="${empty nextFile}">
					<a class="btn btn-primary disabled" disabled="disabled" href="#">
						<span class="glyphicon glyphicon-step-forward"></span>Next File
					</a>
				</c:when>
				<c:otherwise>
					<c:url value="edit" var="nextFileURL">
					    <c:param name="state" value="${nextFile.state}" />
					    <c:param name="fn" value="${nextFile.webFileName}" />
					</c:url>
					<a class="btn btn-primary needTooltip" data-toggle="tooltip"
						title="${nextFile.state}/${nextFile.fileName}"
						href="${nextFileURL}">
						<span class="glyphicon glyphicon-step-forward"></span>Next File
					</a>
				</c:otherwise>
				</c:choose>
			</td>
	            
			<td style="padding-left: 3in;">
				<small>(Please don't forget to score for evidence of learning.)
				</small>
			</td>
	            
			<td class="nowrap" style="padding-left: 12px;">
				<a id="cmdScrollToSoundness" href="#" class="btn btn-primary btn-xs" role="button">Scroll There &laquo;</a>
			</td>
	        </tr></table>
        </div>
    </div>
    
    </form>
    
    <!-- Training mode: we store it in a hidden table -->
    <c:if test="${trainingMode or verifyMode or assessMode}">
        <div id="trainingScriptContainer" style="display:none;">
        <c:if test="${verifyMode}">
            <div id="trainingIsVerify"></div>
        </c:if>
        <c:if test="${assessMode}">
            <div id="trainingIsAssess"></div>
        </c:if>
        <table id="trainingScript"><tbody>
            <c:forEach items="${trainerTranscript.transcriptItems}" var="item" varStatus="status">
                <tr data-trainindex="${status.index}">
                    <td class="trainAct"><c:out value="${item.dialogAct}" /></td>
                    <td class="trainSubAct"><c:out value="${item.dialogSubAct}" /></td>
                    <td class="trainMode"><c:out value="${item.dialogMode}" /></td>
                    <td class="trainComments"><c:out value="${item.comments}" /></td>
                </tr>
            </c:forEach>
        </tbody></table>
        </div>
    </c:if>
    
    <!-- Editor for row editing -->
    <div id="taggingDlgContainer" style="display:none;">
    <div id="taggingDlg" class="panel panel-primary taggingDlgClass">
        <div class="panel-heading" id="editDlgHeader" >
            <h3 class="pull-left panel-title">Tagging Information</h3>
            <button type="button" id="dialogHelpRequest" class="pull-right btn btn-xs btn-default needPopover" 
                data-container="body" data-toggle="popover" data-placement="left"
                data-content="Please select a Dialog Act and a corresponding sub-act.  You should
                supply a Dialog Mode if the mode changes with this line. You
                may optionally provide a comment.  Hint: if you are currently
                tagging a row, you can use ALT+N to save your changes and begin
                tagging the next row. You may also
                use ALT+P to move to the previous row."
            >?</button>
            <div class="clearfix">&nbsp;</div>
        </div>
        <!-- Note: no panel-body  -->
        <table class="table-condensed borderless" style="width: 100%;">
            <tr>
                <td class="nowrap"><label for="editDlgAct">Dialog Act</label></td>
                <td>
                    <select id="editDlgAct" tabindex="1" class="editControl">
                        <option value=""></option>
                        <c:forEach items="${taxonomy.dialogActs}" var="act">
                            <option value="${act.name}">${act.name}</option>
                        </c:forEach>
                    </select>
                </td>
                
                <td rowspan="3" style="padding-left: 32px; width:100%;">
                    <label for="editDlgComments">Comments</label><br/>
                    <textarea id="editDlgComments" tabindex="4" rows="4" class="editControl" style="width:100%;"></textarea>
                </td>
            </tr>
            <tr>
                <td class="nowrap"><label for="editDlgSubAct">Dialog Sub-Act</label></td>
                <td>
                    <select id="editDlgSubAct" tabindex="2" class="editControl">
                    </select>
                </td>
                <td></td>
            </tr>
            <tr>
                <td class="nowrap"><label for="editDlgMode">Dialog Mode</label></td>
                <td>
                    <select id="editDlgMode" tabindex="3" class="editControl">
                        <option value=""></option>
                        <c:forEach items="${taxonomy.dialogModes}" var="mode">        
                            <option value="${mode}">${mode}</option>
                        </c:forEach>
                    </select>
                </td>
                <td></td>
            </tr>
            <tr>
                <td class="nowrap" colspan="3">
                    <label>
                        <input id="editDlgTagConf" type="checkbox" tabindex="5" accesskey="C">
                        I am NOT sure that this is correct (ALT+C)
                    </label>
                </td>
            </tr>
            <tr>
                <td colspan="3">
                    <div id="editDlgTrainingDisplay"></div>
                </td>
            </tr>
        </table>
        <div class="panel-footer" id="editDlgFooter">
            <div>
            <div class="pull-left">
                <button id="dialogEditorPrevRow" class="btn btn-info btn-xs needTooltip"
                        data-toggle="tooltip" data-placement="bottom" 
                        title="Save any changes and begin tagging the previous row (ALT+P)">
                    <span class="glyphicon glyphicon-arrow-up"></span> Prev
                </button>
                <button id="dialogEditorNextRow" class="btn btn-info btn-xs needTooltip"
                        data-toggle="tooltip" data-placement="bottom"
                        title="Save any changes and begin tagging the next row (ALT+N)">
                    <span class="glyphicon glyphicon-arrow-down"></span> Next
                </button>
                <c:if test="${trainingMode}">
                    &nbsp;&nbsp;
                    <button id="dialogEditorTrainReveal" class="btn btn-info btn-xs needTooltip"
                        data-toggle="tooltip"  data-placement="bottom"
                        title="Reveal the Act, SubAct, and Mode the trainer specified">
                        <span class="glyphicon glyphicon-eye-open"></span> Show Training Data
                    </button>
                </c:if>
                <c:if test="${verifyMode}">
                    &nbsp;&nbsp;
                    <button id="dialogEditorTrainReveal" class="btn btn-info btn-xs needTooltip"
                        data-toggle="tooltip"  data-placement="bottom"
                        title="Show other tagging data and comments">
                        <span class="glyphicon glyphicon-eye-open"></span> Show Extra Tag
                    </button>
                </c:if>
                <c:if test="${assessMode}">
                    &nbsp;&nbsp;
                    <button id="dialogEditorTrainReveal" class="btn btn-info btn-xs needTooltip"
                        data-toggle="tooltip"  data-placement="bottom"
                        title="Show other tagging data and comments">
                        <span class="glyphicon glyphicon-eye-open"></span> Show Backing
                    </button>
                </c:if>
            </div>

            <div class="pull-right">
                <button id="dialogEditorSave" class="btn btn-success btn-xs needTooltip"
                        data-toggle="tooltip"  data-placement="bottom"
                        title="Accept the tagging changes made to this row">
                    <span class="glyphicon glyphicon-ok-circle"></span> Done
                </button>
                <button id="dialogEditorCancel" class="btn btn-warning btn-xs needTooltip"
                        data-toggle="tooltip" data-placement="bottom" 
                        title="Discard any tagging changes made to this row">
                    <span class="glyphicon glyphicon-remove-circle"></span> Cancel
                </button>
            </div>
            
            <div class="clearfix">&nbsp;</div>
            </div>
        </div>
    </div>
    </div>
    
    </jsp:body>
</t:basepage>

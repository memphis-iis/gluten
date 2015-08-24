// Application-wide javascript
// Note that code here can assume that both lodash and jQuery are present

//Initial startup actions to make our life easier
$(function(){
    //Handle bootstrap drop-downs
    $('.dropdown-toggle').dropdown();

    //Automatically set up jQuery DataTables if they have the right class
    $('.data-table').DataTable();

    //If form controls have an ID, but not a name - set their name attribute
    $("input, select, textarea, button, datalist, keygen, output").each(function(idx, val){
        var ele = $(val);
        if (_.isEmpty(ele.attr('name'))) {
            ele.attr('name', ele.attr('id'));
        }
    });

    //If a table cell requests truncation, then do so
    $("td.trunc-text").each(function(idx, ele){
        ele = $(ele);
        ele.text(_.trunc(_.trim(ele.text()), 30));
    });
});

////////////////////////////////////////////////////////////////////////
//jQuery add-ons

//Helper for jQuery to see if a query returns anything
$.fn.exists = function () {
    return this.length && this.length !== 0;
};

////////////////////////////////////////////////////////////////////////
//Simple helper functions

helper = {
    //Helper to get always-valid, always-trimmed string
    toTrimStr: function(s) {
        var ss = "" + s;
        if (ss && ss != "undefined" && ss.length && ss.length > 0) {
            return _.trim(ss);
        }
        else {
            return "";
        }
    },

    //Return true if obj is a function
    //Stolen from underscore
    isFunc: function(obj) {
        return !!(obj && obj.constructor && obj.call && obj.apply);
    },

    //Helper to always return an integer value from s (or def if that's not possible)
    safeParseInt: function(s, def) {
        try {
            var i = parseInt(helper.toTrimStr(s));
            if (isNaN(i))
                return def;
            return i;
        }
        catch(e) {
            return def;
        }
    },

    //Hides everything with class "opthide", then adds a "Click to Reveal"
    //sibling (with class opthideClick)
    setupOptHidden: function() {
        $(".opthide").each(function(index, value) {
            var clicker = $("<div></div>")
                .addClass("opthideClick")
                .html("<a>Click to Reveal</a>");
            $(value).parent().append(clicker);
        });

        $(".opthide").hide();

        $(".opthideClick").click(function(e) {
            e.preventDefault();
            $(this).parent().find(".opthide").show();
            $(this).remove();
        });
    },

    hexByte: function(b) {
        var s = "00" + Math.round(b).toString(16);
        return s.substring(s.length - 2);
    },

    /* accepts parameters
     * h  Object = {h:x, s:y, v:z}
     * OR
     * h, s, v
     * Note that 0 <= h,s,v <= 1
    */
    HSVtoRGB: function(h, s, v) {
        var r, g, b, i, f, p, q, t;
        if (h && s === undefined && v === undefined) {
            s = h.s; v = h.v; h = h.h;
        }
        i = Math.floor(h * 6);
        f = h * 6 - i;
        p = v * (1 - s);
        q = v * (1 - f * s);
        t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
            //Just punt and use case 0 - this is mainly for warnings in
            //js-lint-type editors
            default: r = v; g = t; b = p; break;
        }

        var hb = helper.hexByte;
        return "#" + hb(r*255) + hb(g*255) + hb(b*255);
        /*
        return {
            r: Math.floor(r * 255),
            g: Math.floor(g * 255),
            b: Math.floor(b * 255)
        };
        */
    },

    isTouchBrowser: function() {
        return !!('ontouchstart' in document.documentElement) ||
               !!('ontouchstart' in window) ||
               !!(navigator.msMaxTouchPoints);
    },

    endsWith: function(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    },

    stripSuffix: function(str, suffix) {
        if (helper.endsWith(str, suffix)) {
            return str.slice(0, -suffix.length);
        }
        else {
            return str;
        }
    },

    clearDispMessage: function() {
        $("#helperDispMessageFozzyWuzHere").remove();
    },

    dispMessage: function(success, msgText) {
        helper.clearDispMessage();

        //Note our reliance on the class helperDispMessageBox being defined
        //in annotator.css (or somewhere else)
        var cls = success ? "alert-success" : "alert-danger";
        var message = $('<div id="helperDispMessageFozzyWuzHere" ' +
                'class="alert ' + cls + ' helperDispMessageBox" ' +
                'style="display: none;">');
        var close = $('<button type="button" class="close" data-dismiss="alert">&times</button>');
        message.append(close); // adding the close button to the message
        message.append(msgText);

        var delay = success ? 2000: 10000;
        message.appendTo($('body')).fadeIn(250).delay(delay).fadeOut(250);
        $(".alert").alert();
    }
};

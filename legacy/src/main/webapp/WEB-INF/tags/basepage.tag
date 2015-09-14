<%@ tag language="java" pageEncoding="UTF-8"%>

<%@attribute name="pagetitle" fragment="true" %>
<%@attribute name="head" fragment="true" required="false" %>
<%@attribute name="scripts" fragment="true" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><jsp:invoke fragment="pagetitle"/></title>
    
    <link rel="icon" type="image/x-icon" href="http://www.memphis.edu/favicon.ico" />

    <!-- Bootstrap -->
    <link rel="stylesheet" href="https://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">

    <!-- jQuery UI theme -->
    <link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.2/themes/smoothness/jquery-ui.min.css">
    
    <!-- DataTables CSS -->
    <link rel="stylesheet" type="text/css" href="http://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/css/jquery.dataTables.css">

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
    <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->
    
    <!-- Our styles -->
    <link rel="stylesheet" type="text/css" href="css/annotator.css">
    
    <jsp:invoke fragment="head"/>
</head>

<body>
<!-- MAIN CONTENT -->
<div class="container-fluid">
    <jsp:doBody/>
    
    <hr>

    <!-- FOOTER, ENDMATTER, and STATUS -->
    <footer>
        <img style="float:left;" class="img-responsive img-rounder" src="http://www.memphis.edu/shared/shared_rootsite/images/brandnew.jpg">
        <p style="float:right;"><small>Institute for Intelligent Systems, 2014</small></p>
        <br style="clear:both">
    </footer>
    <div class="row">
        <div class="col-md-4"><div id="query_progress_txt"></div></div>
        <div class="col-md-8"><div id="query_progress_bar"></div></div>
    </div>
</div> <!-- /container --> 

<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.2/jquery-ui.min.js"></script>
 
<!-- bootstrap -->
<script src="https://netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>

<!-- DataTables -->
<script type="text/javascript" charset="utf8" src="http://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/jquery.dataTables.min.js"></script>

<!-- local utilities -->
<script src="js/helpers.js"></script>
<script src="js/Encoder.js"></script>

<!-- local scripts -->
<jsp:invoke fragment="scripts"/>

</body>
</html>
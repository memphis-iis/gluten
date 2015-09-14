/**
 * The main (and top-level) package in this web application.  Mainly contains
 * the servlets (and their helper base class).  Note that  we use the servlet
 * annotations introduced in the 3.0 Servlet spec, so you won't find the 
 * servlets defined in web.xml .  As of this writing, the servlets are:
 * 
 * <ul>
 *     <li>The main servlet (for the home page)
 *     <li>The edit servlet (for the edit/save/view) page
 *     <li>The taxonomy servlet for serving our discourse taxonomy to JSON clients
 *     <li>The OAuth 2 callback servlet for logins
 * </ul>
 * 
 * <p>In addition, this package contains some other odds and ends:
 * 
 * <ul>
 *     <li>The servlet base class used by most of our servlets 
 *     <li>The exceptions that our servlets throw when they want the
 *         web user sent to an error screen 
 *     <li>Constants (in the Const) class 
 *     <li>Static utility functions (in the Utils class)
 * </ul>
 */
package edu.memphis.iis.tdc.annotator;
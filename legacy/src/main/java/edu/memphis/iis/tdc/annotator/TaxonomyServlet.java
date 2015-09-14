package edu.memphis.iis.tdc.annotator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.memphis.iis.tdc.annotator.config.ConfigContext;
import edu.memphis.iis.tdc.annotator.model.DialogAct;
import edu.memphis.iis.tdc.annotator.model.Taxonomy;

/**
 * Serve our taxonomy data as JavaScript-embedded JSON.  Note that we have
 * access to a Taxonomy instance in our server-side JSTL pages (from
 * ServletBase). This is for allowing client JS to access the taxonomy
 * (e.g. the JS from edit.jsp)
 */
@WebServlet(value="/taxonomy", loadOnStartup=3)
public class TaxonomyServlet extends ServletBase {
    private static final long serialVersionUID = 1L;

    @Override
    protected String doProtectedGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        response.setContentType("application/javascript");
        
        Taxonomy tax = ConfigContext.getInst().getTaxonomy();
        
        List<String> actNames = new ArrayList<String>();            
        Map<String, JSONObject> acts = new HashMap<String, JSONObject>();
        
        for(DialogAct da: tax.getDialogActs()) {
            actNames.add(da.getName());
            
            JSONObject d = new JSONObject();
            d.put("name", da.getName());
            d.put("subtypes", new JSONArray(da.getSubtypes()));
            acts.put(da.getName(), d);
        }
        
        JSONObject root = new JSONObject();
        root.put("dialogModes", tax.getDialogModes());
        root.put("dialogActNames", actNames);
        root.put("dialogActs", acts);
        
        //Gotta love exception-free int parsing
        int indent = NumberUtils.toInt(request.getParameter("indent"));
        if (indent == 0)
            indent = 4;
        
        PrintWriter output = response.getWriter();
        output.write("var taxonomy = ");
        output.write(root.toString(indent));
        output.write(";");
        output.flush();
        return NO_VIEW;
    }
    
    @Override
    protected String doProtectedPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UserErrorException
    {
        throw new UserErrorException("Something is wrong - the HTTP action is supported for this page");
    }
}

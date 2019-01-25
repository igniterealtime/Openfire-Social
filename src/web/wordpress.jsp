<%@ page import="java.util.*" %>
<%@ page import="org.ifsoft.wordpress.openfire.*" %>
<%@ page import="org.jivesoftware.openfire.*" %>
<%@ page import="org.jivesoftware.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%

    boolean update = request.getParameter("update") != null;
    String errorMessage = null;

    // Get handle on the plugin
    PluginImpl plugin = (PluginImpl) XMPPServer.getInstance().getPluginManager().getPlugin("wordpress");

    if (update)
    {                               
        String wordpressEnabled = request.getParameter("wordpressEnabled");
        JiveGlobals.setProperty("wordpress.enabled", (wordpressEnabled != null && wordpressEnabled.equals("on")) ? "true": "false");  
        
        String wpAuthProviderEnabled = request.getParameter("wpAuthProviderEnabled");
        JiveGlobals.setProperty("wordpress.auth.provider.enabled", (wpAuthProviderEnabled != null && wpAuthProviderEnabled.equals("on")) ? "true": "false");         
        
        String wpUserProviderEnabled = request.getParameter("wpUserProviderEnabled");
        JiveGlobals.setProperty("wordpress.user.provider.enabled", (wpUserProviderEnabled != null && wpUserProviderEnabled.equals("on")) ? "true": "false");         

        String wpGroupProviderEnabled = request.getParameter("wpGroupProviderEnabled");
        JiveGlobals.setProperty("wordpress.group.provider.enabled", (wpGroupProviderEnabled != null && wpGroupProviderEnabled.equals("on")) ? "true": "false");                 
    }

%>
<html>
<head>
   <title><fmt:message key="plugin.title.description" /></title>

   <meta name="pageID" content="wordpress-settings"/>
</head>
<body>
<% if (errorMessage != null) { %>
<div class="error">
    <%= errorMessage%>
</div>
<br/>
<% } %>

<div class="jive-table">
<form action="wordpress.jsp" method="post">
    <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.settings.description"/></th>
            </tr>
            </thead>
            <tbody>  
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="wordpressEnabled"<%= (JiveGlobals.getProperty("wordpress.enabled", "true").equals("true")) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.enabled" />       
                </td>  
            </tr>   
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="wpAuthProviderEnabled"<%= (JiveGlobals.getProperty("wordpress.auth.provider.enabled", "false").equals("true")) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.auth.enabled" />       
                </td>  
            </tr>   
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="wpUserProviderEnabled"<%= (JiveGlobals.getProperty("wordpress.user.provider.enabled", "false").equals("true")) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.user.enabled" />       
                </td>  
            </tr>
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="wpGroupProviderEnabled"<%= (JiveGlobals.getProperty("wordpress.group.provider.enabled", "false").equals("true")) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.group.enabled" />       
                </td>  
            </tr>            
            </tbody>
        </table>
    </p>
   <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.configuration.save.title"/></th>
            </tr>
            </thead>
            <tbody>         
            <tr>
                <th colspan="2"><input type="submit" name="update" value="<fmt:message key="config.page.configuration.submit" />"><fmt:message key="config.page.configuration.restart.warning"/></th>
            </tr>       
            </tbody>            
        </table> 
    </p>
</form>
</div>
</body>
</html>

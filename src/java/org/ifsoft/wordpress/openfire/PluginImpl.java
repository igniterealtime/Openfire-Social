/*
 * Copyright (C) 2018 Ignite Realtime. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ifsoft.wordpress.openfire;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.DispatcherType;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.XMPPServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jivesoftware.util.*;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.fcgi.server.proxy.*;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;


public class PluginImpl implements Plugin, PropertyEventListener
{
    private static final Logger Log = LoggerFactory.getLogger(PluginImpl.class);
    private static final String ourHostname = XMPPServer.getInstance().getServerInfo().getHostname();

    private PluginImpl plugin;
    private ServletContextHandler context;

    public void destroyPlugin()
    {
        Log.info("destroyPlugin");

        HttpBindManager.getInstance().removeJettyHandler(context);

        cleanupWordPress();

        PropertyEventDispatcher.removeListener(this);
    }

    public void initializePlugin(final PluginManager manager, final File pluginDirectory)
    {
        plugin = this;

        boolean wordpressEnabled = JiveGlobals.getBooleanProperty("wordpress.enabled", false);

        if (wordpressEnabled)
        {
            PropertyEventDispatcher.addListener(this);

            Log.info("Initialize wordpress WebService ");

            context = new ServletContextHandler(null, "/wp", ServletContextHandler.SESSIONS);
            context.setClassLoader(this.getClass().getClassLoader());
            context.setResourceBase(pluginDirectory.getPath() + "/classes");
            context.setWelcomeFiles(new String[]{"index.php"});

            FilterHolder tryHolder = new FilterHolder(new TryFilesFilter());
            tryHolder.setInitParameter("files", "$path /index.php?p=$path");
            context.addFilter(tryHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

            ServletHolder defHolder = new ServletHolder("default",new DefaultServlet());
            defHolder.setAsyncSupported(true);
            defHolder.setInitParameter("dirAllowed","false");
            context.addServlet(defHolder,"/");

            ServletHolder fgciHolder = new ServletHolder("fcgi", new FastCGIProxyServlet());
            fgciHolder.setAsyncSupported(true);
            fgciHolder.setInitParameter("proxyTo","http://localhost:9123");
            fgciHolder.setInitParameter("prefix","/");
            fgciHolder.setInitParameter("scriptRoot", pluginDirectory.getPath() + "/classes");
            fgciHolder.setInitParameter("scriptPattern","(.+?\\.php)");
            context.addServlet(fgciHolder, "*.php");

            HttpBindManager.getInstance().addJettyHandler(context);

            setupWordPress();

        } else {
            Log.info("wordpress disabled");
        }
    }

    private String getIpAddress()
    {
        String ourIpAddress = "127.0.0.1";

        try {
            ourIpAddress = InetAddress.getByName(ourHostname).getHostAddress();
        } catch (Exception e) {

        }

        return ourIpAddress;
    }

    private void setupWordPress()
    {
        Log.info("Setting WordPress as new auth Provider");

        JiveGlobals.setProperty("jdbcAuthProvider.passwordSQL", "SELECT user_pass FROM wp_users WHERE user_login=?");
        JiveGlobals.setProperty("jdbcAuthProvider.setPasswordSQL", "");
        JiveGlobals.setProperty("jdbcAuthProvider.allowUpdate", "false");
        JiveGlobals.setProperty("jdbcAuthProvider.passwordType", "md5");
        JiveGlobals.setProperty("jdbcAuthProvider.useConnectionProvider", "true");

        JiveGlobals.setProperty("provider.auth.className",  "org.jivesoftware.openfire.auth.JDBCAuthProvider");

        Log.info("Setting WordPress as user Provider");

        JiveGlobals.setProperty("jdbcUserProvider.loadUserSQL", "SELECT user_login, display_name, user_email FROM wp_users WHERE user_login=?");
        JiveGlobals.setProperty("jdbcUserProvider.userCountSQL", "SELECT COUNT(*) FROM wp_users");
        JiveGlobals.setProperty("jdbcUserProvider.allUsersSQL", "SELECT user_login FROM wp_users");
        JiveGlobals.setProperty("jdbcUserProvider.searchSQL", "SELECT user_login FROM wp_users WHERE");
        JiveGlobals.setProperty("jdbcUserProvider.user_loginField", "user_login");
        JiveGlobals.setProperty("jdbcUserProvider.nameField", "display_name");
        JiveGlobals.setProperty("jdbcUserProvider.emailField", "user_email");
        JiveGlobals.setProperty("jdbcUserProvider.useConnectionProvider", "true");

        JiveGlobals.setProperty("provider.user.className",  "org.jivesoftware.openfire.user.JDBCUserProvider");

        Log.info("Setting WordPress as group Provider");

        JiveGlobals.setProperty("jdbcGroupProvider.groupCountSQL", "SELECT count(*) FROM wp_bp_groups");
        JiveGlobals.setProperty("jdbcGroupProvider.allGroupsSQL", "SELECT name FROM wp_bp_groups");
        JiveGlobals.setProperty("jdbcGroupProvider.userGroupsSQL", "SELECT name FROM wp_bp_groups INNER JOIN wp_bp_groups_members ON wp_bp_groups.id = wp_bp_groups_members.group_id WHERE wp_bp_groups_members.user_id IN (SELECT ID FROM wp_users WHERE user_login=?) AND is_confirmed=1");
        JiveGlobals.setProperty("jdbcGroupProvider.descriptionSQL", "SELECT description FROM wp_bp_groups WHERE name=?");
        JiveGlobals.setProperty("jdbcGroupProvider.loadMembersSQL", "SELECT user_login FROM wp_users INNER JOIN wp_bp_groups_members ON wp_users.ID = wp_bp_groups_members.user_id WHERE wp_bp_groups_members.group_id IN (SELECT id FROM wp_bp_groups WHERE name=?) AND user_login<>'admin' AND is_confirmed=1");
        JiveGlobals.setProperty("jdbcGroupProvider.loadAdminsSQL", "SELECT user_login FROM wp_users INNER JOIN wp_bp_groups_members ON wp_users.ID = wp_bp_groups_members.user_id WHERE wp_bp_groups_members.group_id IN (SELECT id FROM wp_bp_groups WHERE name=?) AND user_login='admin' AND is_confirmed=1");
        JiveGlobals.setProperty("jdbcGroupProvider.useConnectionProvider", "true");

        JiveGlobals.setProperty("provider.group.className",  "org.jivesoftware.openfire.group.JDBCGroupProvider");

        JiveGlobals.setProperty("cache.groupMeta.maxLifetime", "60000");
        JiveGlobals.setProperty("cache.group.maxLifetime", "60000");
        JiveGlobals.setProperty("cache.userCache.maxLifetime", "60000");
    }

    private void cleanupWordPress()
    {
        Log.info("Cleanup WordPress as new auth Provider");

        JiveGlobals.deleteProperty("jdbcAuthProvider.passwordSQL");
        JiveGlobals.deleteProperty("jdbcAuthProvider.setPasswordSQL");
        JiveGlobals.deleteProperty("jdbcAuthProvider.allowUpdate");
        JiveGlobals.deleteProperty("jdbcAuthProvider.passwordType");
        JiveGlobals.deleteProperty("jdbcAuthProvider.useConnectionProvider");

        JiveGlobals.setProperty("provider.auth.className",  "org.jivesoftware.openfire.auth.DefaultAuthProvider");

        Log.info("Cleanup WordPress as user Provider");

        JiveGlobals.deleteProperty("jdbcUserProvider.loadUserSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.userCountSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.allUsersSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.searchSQL");
        JiveGlobals.deleteProperty("jdbcUserProvider.user_loginField");
        JiveGlobals.deleteProperty("jdbcUserProvider.nameField");
        JiveGlobals.deleteProperty("jdbcUserProvider.emailField");
        JiveGlobals.deleteProperty("jdbcUserProvider.useConnectionProvider");

        JiveGlobals.setProperty("provider.user.className",  "org.jivesoftware.openfire.user.DefaultUserProvider");

        Log.info("Cleanup WordPress as group Provider");

        JiveGlobals.deleteProperty("jdbcGroupProvider.groupCountSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.allGroupsSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.userGroupsSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.descriptionSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.loadMembersSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.loadAdminsSQL");
        JiveGlobals.deleteProperty("jdbcGroupProvider.useConnectionProvider");

        JiveGlobals.setProperty("provider.group.className",  "org.jivesoftware.openfire.group.DefaultGroupProvider");

        JiveGlobals.deleteProperty("cache.groupMeta.maxLifetime");
        JiveGlobals.deleteProperty("cache.group.maxLifetime");
        JiveGlobals.deleteProperty("cache.userCache.maxLifetime");
    }

    //-------------------------------------------------------
    //
    //  PropertyEventListener
    //
    //-------------------------------------------------------


    public void propertySet(String property, Map params)
    {

    }

    public void propertyDeleted(String property, Map<String, Object> params)
    {

    }

    public void xmlPropertySet(String property, Map<String, Object> params) {

    }

    public void xmlPropertyDeleted(String property, Map<String, Object> params) {

    }

}


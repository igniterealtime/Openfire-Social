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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.bind.DatatypeConverter;
import javax.servlet.*;
import javax.servlet.http.*;
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
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.webapp.WebAppContext;

import org.eclipse.jetty.util.security.*;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.*;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;


public class PluginImpl implements Plugin, PropertyEventListener
{
    private static final Logger Log = LoggerFactory.getLogger(PluginImpl.class);
    private static final String ourHostname = XMPPServer.getInstance().getServerInfo().getHostname();

    private PluginImpl plugin;
    private WebAppContext context;


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

        boolean wordpressEnabled = JiveGlobals.getBooleanProperty("wordpress.enabled", true);

        if (wordpressEnabled)
        {
            try {
                PropertyEventDispatcher.addListener(this);

                Log.info("Initialize wordpress WebService ");

                context = new WebAppContext();
                context.setContextPath("/");

                context.setClassLoader(this.getClass().getClassLoader());
                context.setResourceBase(pluginDirectory.getPath() + "/classes");
                context.setWelcomeFiles(new String[]{"index.php"});

                FilterHolder tryHolder = new FilterHolder(new TryFilesFilter());
                tryHolder.setInitParameter("files", "$path /index.php?p=$path");
                context.addFilter(tryHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

                ServletHolder defHolder = new ServletHolder("default", new DefaultServlet());
                defHolder.setAsyncSupported(true);
                defHolder.setInitParameter("dirAllowed","false");
                context.addServlet(defHolder,"/");

                ServletHolder fgciHolder = new ServletHolder("fcgi", new FastCGIProxyServlet());
                fgciHolder.setAsyncSupported(true);
                fgciHolder.setInitParameter("proxyTo","http://localhost:9000");
                fgciHolder.setInitParameter("prefix","/");
                fgciHolder.setInitParameter("scriptRoot", pluginDirectory.getPath() + "/classes");
                fgciHolder.setInitParameter("scriptPattern","(.+?\\.php)");

                context.addServlet(fgciHolder, "*.php");
                context.setSecurityHandler(basicAuth("wordpress"));
                HttpBindManager.getInstance().addJettyHandler(context);

                setupWordPress(pluginDirectory.getPath() + "/classes");

            } catch (Exception e) {
                Log.error("wordpress error", e);
            }

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

    private static final SecurityHandler basicAuth(String realm) {

        OpenfireLoginService l = new OpenfireLoginService();
        l.setName(realm);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"wordpress"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName(realm);
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);

        return csh;
    }

    private void setupWordPress(String homePath) throws IOException
    {
        boolean wpAuthProviderEnabled = JiveGlobals.getBooleanProperty("wordpress.auth.provider.enabled", false);

        if (wpAuthProviderEnabled)
        {
            Log.info("Setting WordPress as new auth Provider");

            JiveGlobals.setProperty("jdbcAuthProvider.passwordSQL", "SELECT user_pass FROM wp_users WHERE user_login=?");
            JiveGlobals.setProperty("jdbcAuthProvider.setPasswordSQL", "");
            JiveGlobals.setProperty("jdbcAuthProvider.allowUpdate", "false");
            JiveGlobals.setProperty("jdbcAuthProvider.passwordType", "md5");
            JiveGlobals.setProperty("jdbcAuthProvider.useConnectionProvider", "true");

            JiveGlobals.setProperty("provider.auth.className",  "org.jivesoftware.openfire.auth.JDBCAuthProvider");
        }

        boolean wpUserProviderEnabled = JiveGlobals.getBooleanProperty("wordpress.user.provider.enabled", false);

        if (wpUserProviderEnabled)
        {
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
        }

        boolean wpGroupProviderEnabled = JiveGlobals.getBooleanProperty("wordpress.group.provider.enabled", false);

        if (wpGroupProviderEnabled)
        {
            Log.info("Setting WordPress as group Provider");

            JiveGlobals.setProperty("jdbcGroupProvider.groupCountSQL", "SELECT count(*) FROM wp_bp_groups");
            JiveGlobals.setProperty("jdbcGroupProvider.allGroupsSQL", "SELECT name FROM wp_bp_groups");
            JiveGlobals.setProperty("jdbcGroupProvider.userGroupsSQL", "SELECT name FROM wp_bp_groups INNER JOIN wp_bp_groups_members ON wp_bp_groups.id = wp_bp_groups_members.group_id WHERE wp_bp_groups_members.user_id IN (SELECT ID FROM wp_users WHERE user_login=?) AND is_confirmed=1");
            JiveGlobals.setProperty("jdbcGroupProvider.descriptionSQL", "SELECT description FROM wp_bp_groups WHERE name=?");
            JiveGlobals.setProperty("jdbcGroupProvider.loadMembersSQL", "SELECT user_login FROM wp_users INNER JOIN wp_bp_groups_members ON wp_users.ID = wp_bp_groups_members.user_id WHERE wp_bp_groups_members.group_id IN (SELECT id FROM wp_bp_groups WHERE name=?) AND user_login<>'admin' AND is_confirmed=1");
            JiveGlobals.setProperty("jdbcGroupProvider.loadAdminsSQL", "SELECT user_login FROM wp_users INNER JOIN wp_bp_groups_members ON wp_users.ID = wp_bp_groups_members.user_id WHERE wp_bp_groups_members.group_id IN (SELECT id FROM wp_bp_groups WHERE name=?) AND user_login='admin' AND is_confirmed=1");
            JiveGlobals.setProperty("jdbcGroupProvider.useConnectionProvider", "true");

            JiveGlobals.setProperty("provider.group.className",  "org.jivesoftware.openfire.group.JDBCGroupProvider");
        }

        JiveGlobals.setProperty("cache.groupMeta.maxLifetime", "60000");
        JiveGlobals.setProperty("cache.group.maxLifetime", "60000");
        JiveGlobals.setProperty("cache.userCache.maxLifetime", "60000");

        Log.info("Creating WordPress wp-config.php file");

        List<String> lines = new ArrayList<String>();
        lines.add( "<?php" );
        lines.add( "define('DB_NAME', '" + of_get_db_name() + "');" );
        lines.add( "define('DB_USER', '" + of_get_db_username() + "');" );
        lines.add( "define('DB_PASSWORD', '" + of_get_db_password() + "');" );
        lines.add( "define('DB_HOST', '127.0.0.1:3306');" );
        lines.add( "define('DB_CHARSET', 'utf8');" );
        lines.add( "define('DB_COLLATE', '');" );
        lines.add( "define('AUTH_KEY',         'c49422521a0a7833b0144a1a2d6476d2be408a0699e8310dfe5600bc0810cd50');" );
        lines.add( "define('SECURE_AUTH_KEY',  '6bfc05857d3fe055396ab3b99ce297e21ab71c32b8b85f3ea70620d1745a38a3');" );
        lines.add( "define('LOGGED_IN_KEY',    'ce8c6524d966f5047a1ed43eccd7898ae084f88c2f53ee24e82a634e143c432e');" );
        lines.add( "define('NONCE_KEY',        '582fef1a3f72fa1d3f7833c9de2afed64d41b4a7f3c74ee8d3647e68bd4d1967');" );
        lines.add( "define('AUTH_SALT',        'ffea16f44604876accf416c9b3d538191f9c2c7a1ce7ad6ada52ae58b44fff41');" );
        lines.add( "define('SECURE_AUTH_SALT', '2ade2a639d1ae33f84f6cde82dd5e97ff918f309fedc532496b7c1745de00e4d');" );
        lines.add( "define('LOGGED_IN_SALT',   'a9fd9b787ca536308b06e07fca069fc4f4023a15097b0ed12bbc75f65631ecf5');" );
        lines.add( "define('NONCE_SALT',       '754d566bbd557584ef1cb96ecbb7d328edf1fc795a0c515ca5cefbf704ad5d55');" );
        lines.add( "$table_prefix  = 'wp_';" );
        lines.add( "define('WP_DEBUG', true);" );
        lines.add( "define('WP_DEBUG_LOG', true);" );
        lines.add( "define('WP_DEBUG_DISPLAY', true);" );
        lines.add( "$_SERVER['HTTP_HOST'] = '" + ourHostname + ":" + JiveGlobals.getProperty("httpbind.port.secure", "7443") + "';" );
        lines.add( "define('WP_SITEURL', 'https://' . $_SERVER['HTTP_HOST'] . '');" );
        lines.add( "define('WP_HOME', 'https://' . $_SERVER['HTTP_HOST'] . '');" );
        lines.add( "if ( !defined('ABSPATH') ) define('ABSPATH', dirname(__FILE__) . '/');" );
        lines.add( "require_once(ABSPATH . 'wp-settings.php');" );
        lines.add( "if ( !defined( 'WP_CLI' ) ) { add_filter('wp_headers', function($headers) { unset($headers['X-Pingback']); return $headers; }); add_filter( 'xmlrpc_methods', function( $methods ) { unset( $methods['pingback.ping'] ); return $methods; }); add_filter( 'auto_update_translation', '__return_false' ); }");

        Path file = Paths.get(homePath + "/wp-config.php");
        Files.write(file, lines, Charset.forName("UTF-8"));
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
    //  The old PHP2Java library
    //
    //-------------------------------------------------------

    public String of_get_db_username()
    {
        return JiveGlobals.getXMLProperty("database.defaultProvider.username");
    }

    public String of_get_db_password()
    {
        return JiveGlobals.getXMLProperty("database.defaultProvider.password");
    }

    public String of_get_db_name()
    {
        String serverURL = JiveGlobals.getXMLProperty("database.defaultProvider.serverURL");
        String defaultName = "openfire";

        int pos = serverURL.indexOf("3306");

        if (pos > -1) defaultName = serverURL.substring(pos + 5);

        pos = defaultName.indexOf("?");

        if (pos > -1) defaultName = defaultName.substring(0, pos);

        return defaultName;
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


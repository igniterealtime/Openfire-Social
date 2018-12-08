<?php
/*
Plugin Name: Openfire Plugin
Plugin URI: http://igniterealtime.org
Description: This plugin integrates wordpress with openfire.
Version: 0.1
Author: igniterealtime.org
Author URI: http://igniterealtime.org
*/


if ( !function_exists('wp_hash_password') ) :

    function wp_hash_password($password) {
        global $wp_hasher;

        if ( empty($wp_hasher) ) {
            require_once( ABSPATH . 'wp-includes/class-phpass.php');
            // By default, use the portable hash from phpass
            $wp_hasher = new PasswordHash(8, true);
        }

        //return $wp_hasher->HashPassword( trim( $password ) );
        return md5($password);  // BAO
    }
endif;

//add_filter('show_admin_bar',                '__return_false');
add_filter('wp_authenticate_user',          'ofsocial_login_ok', 10, 2 );
add_action('init',                          'my_plugin_init', 10, 3);
add_action('after_setup_theme',             'my_plugin_init' );

add_action('friends_friendship_accepted',   'ofsocial_create_friendship', 10, 3);
add_action('friends_friendship_deleted',    'ofsocial_delete_friendship', 10, 3);
add_action('groups_group_create_complete',  'ofsocial_create_group');
add_action('groups_join_group',             'ofsocial_join_group', 10, 2);
add_action('groups_leave_group',            'ofsocial_leave_group', 10, 2);
add_action('wp_head',                       'ofsocial_login_check');
add_action('admin_head',                    'ofsocial_user_page');
add_action('login_head',                    'ofsocial_user_page');
add_action('admin_menu',                    'openfire_userimport_menu');


function my_plugin_init()
{
    ?>
    <script>console.log("OPENFIRE - my_plugin_init");</script>
    <?php

    if ( is_user_logged_in() == false )
    {
        if (isset($_SERVER['AUTH_TYPE']) && (strlen($_SERVER['AUTH_TYPE']) > 0))
        {
            $username = "";
            $pass = "";

            list($username, $pass) = explode(':', base64_decode(substr($_SERVER['AUTH_TYPE'], 6)));

            $creds = array();
            $creds['user_login'] = $username;
            $creds['user_password'] = $pass;
            $creds['remember'] = true;

            $user = wp_signon( $creds, false );
            wp_set_current_user( $user->ID, $username);
            wp_set_auth_cookie( $user->ID, false, false );
            do_action( 'wp_login', $username );

            if (is_wp_error($user)) echo "wp_signon ".$user->get_error_message();
        }
    }
}

function openfire_userimport_menu()
{
    add_submenu_page( 'users.php', 'Openfire User Import', 'Import', 'manage_options', 'openfire-user-import', 'openfire_userimport_page');
}

function ofsocial_login_check()
{
    if ( is_user_logged_in())
    {
        $current_user = wp_get_current_user();

        ?>
        <script>console.log("OPENFIRE - ofsocial_login_check <?php echo $current_user->user_login; ?>");</script>
        <?php
    }
}

function ofsocial_user_page()
{
    ?>
    <script>console.log("OPENFIRE - ofsocial_user_page");</script>
    <?php
}

function ofsocial_login_ok( $user, $password )
{
    ?>
    <script>console.log("OPENFIRE - ofsocial_login_ok <?php echo $user->user_login; ?>");</script>
    <?php

     return $user;
}


function ofsocial_join_group($group, $user)
{
    ?>
    <script>console.log("OPENFIRE - ofsocial_join_group");</script>
    <?php
}

function ofsocial_leave_group($group, $user)
{
    ?>
    <script>console.log("OPENFIRE - ofsocial_leave_group");</script>
    <?php
}


function ofsocial_create_friendship($id, $from, $to)
{
    ?>
    <script>console.log("OPENFIRE - ofsocial_create_friendship");</script>
    <?php
}

function ofsocial_delete_friendship($id, $from, $to)
{
    ?>
    <script>console.log("OPENFIRE - ofsocial_delete_friendship");</script>
    <?php
}

function ofsocial_create_group($id)
{
    ?>
    <script>console.log("OPENFIRE - ofsocial_create_group");</script>
    <?php
}

function openfire_userimport_page()
{
    $html_update = "<div class='updated'>All users appear to be have been imported successfully.</div>";

    if (!current_user_can('manage_options')) {
        wp_die( __('You do not have sufficient permissions to access this page.') );
    }

    if ($_POST['mode'] == "submit")
    {
        echo "OPENFIRE - Activated import";
    }

    ?>
    <div class="wrap">
        <?php echo $html_update; ?>
        <div id="icon-users" class="icon32"><br /></div>
        <h2>Openfire User Import</h2>

        <form action="users.php?page=openfire-user-import" method="post">
            <input type="hidden" name="mode" value="submit">
            <input type="submit" value="Import" />
        </form>

        <p style="color: red">Please make sure you back up your database before proceeding!</p>
    </div>

    <?php
}

function echo_log( $what )
{
    echo '<pre>'.print_r( $what, true ).'</pre>';
}
?>
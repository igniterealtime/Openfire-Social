# WordPress
Openfire Auth, Admin, User, Group and vCard providers for WordPress using direct DB access

In order to use this plugin, you **MUST* make a change to your wordpres PHP code(pluggable.php) in order to make user passwords readable in Openfire

```
function wp_hash_password($password) {
    global $wp_hasher;

    if ( empty($wp_hasher) ) {
        require_once( ABSPATH . WPINC . '/class-phpass.php');
        // By default, use the portable hash from phpass
        $wp_hasher = new PasswordHash(8, true);
    }

    //return $wp_hasher->HashPassword( trim( $password ) );
    return md5(trim($password));  
}
endif;
```

# TODO
Use FastCGIProxyServlet to host WordPress php application in Jetty like Nginx does.
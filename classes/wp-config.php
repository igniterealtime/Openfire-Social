<?php
define('DB_NAME', 'openfire_4_3_1');
define('DB_USER', 'root');
define('DB_PASSWORD', 'Abe0kuta181');
define('DB_HOST', '127.0.0.1:3306');
define('DB_CHARSET', 'utf8');
define('DB_COLLATE', '');
define('AUTH_KEY',         'c49422521a0a7833b0144a1a2d6476d2be408a0699e8310dfe5600bc0810cd50');
define('SECURE_AUTH_KEY',  '6bfc05857d3fe055396ab3b99ce297e21ab71c32b8b85f3ea70620d1745a38a3');
define('LOGGED_IN_KEY',    'ce8c6524d966f5047a1ed43eccd7898ae084f88c2f53ee24e82a634e143c432e');
define('NONCE_KEY',        '582fef1a3f72fa1d3f7833c9de2afed64d41b4a7f3c74ee8d3647e68bd4d1967');
define('AUTH_SALT',        'ffea16f44604876accf416c9b3d538191f9c2c7a1ce7ad6ada52ae58b44fff41');
define('SECURE_AUTH_SALT', '2ade2a639d1ae33f84f6cde82dd5e97ff918f309fedc532496b7c1745de00e4d');
define('LOGGED_IN_SALT',   'a9fd9b787ca536308b06e07fca069fc4f4023a15097b0ed12bbc75f65631ecf5');
define('NONCE_SALT',       '754d566bbd557584ef1cb96ecbb7d328edf1fc795a0c515ca5cefbf704ad5d55');
$table_prefix  = 'wp_';
define('WP_DEBUG', true);
define('WP_DEBUG_LOG', true);
define('WP_DEBUG_DISPLAY', true);
$_SERVER['HTTP_HOST'] = 'desktop-545pc5b:7443';
define('WP_SITEURL', 'https://' . $_SERVER['HTTP_HOST'] . '');
define('WP_HOME', 'https://' . $_SERVER['HTTP_HOST'] . '');
if ( !defined('ABSPATH') ) define('ABSPATH', dirname(__FILE__) . '/');
require_once(ABSPATH . 'wp-settings.php');
if ( !defined( 'WP_CLI' ) ) { add_filter('wp_headers', function($headers) { unset($headers['X-Pingback']); return $headers; }); add_filter( 'xmlrpc_methods', function( $methods ) { unset( $methods['pingback.ping'] ); return $methods; }); add_filter( 'auto_update_translation', '__return_false' ); }


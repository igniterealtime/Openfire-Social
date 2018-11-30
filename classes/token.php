<?php
    if (isset($_SERVER['AUTH_TYPE']) && (strlen($_SERVER['AUTH_TYPE']) > 0))
    {
        $username = "";
        $pass = "";

        list($username, $pass) = explode(':', base64_decode(substr($_SERVER['AUTH_TYPE'], 6)));
        echo '{"username": "'.$username.'", "password": "'.$pass.'"}';
    }
?>
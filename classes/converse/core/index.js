window.addEventListener("unload", function()
{

});

window.addEventListener("load", function()
{
    document.title = "Openfire Social";

    fetch("https://" + location.host + "/token.php", {method: "GET"}).then(function(response){ return response.json()}).then(function(token)
    {
        var domain = location.hostname;
        var server = location.host;
        var username = token.username;
        var password = token.password;

        var connUrl = undefined;

        var whitelistedPlugins = ["content", "canned", "info", "screencast"/*, "vmsg"*/, "directory", "search", "ofmeet"]
        var viewMode = 'overlayed';

        var config =
        {
            allow_bookmarks: true,
            allow_non_roster_messaging: true,
            allow_public_bookmarks: true,
            allow_logout: false,
            authentication: "login",
            auto_away: 300,
            auto_xa: 900,
            auto_list_rooms: false,
            auto_login: username != null,
            auto_reconnect: true,
            bosh_service_url: "https://" + server + "/http-bind/",
            debug: false,
            default_domain: domain,
            domain_placeholder: domain,
            hide_open_bookmarks: true,
            i18n: "en",
            jid : username + "@" + domain,
            locked_domain: domain,
            message_archiving: "always",
            message_carbons: true,
            muc_domain: "conference." + domain,
            notify_all_room_messages: false,
            notification_icon: '../image.png',
            password: password,
            play_sounds: true,
            roster_groups: false,
            show_message_load_animation: false,
            sounds_path: 'converse/core/sounds/',
            view_mode: viewMode,
            websocket_url: "wss://" + server + "/ws/",
            ofmeet_modal: false,
            ofmeet_invitation: 'Please join meeting at',
            ofmeet_confirm: 'Meeting?',
            ofmeet_url: 'https://' + server + '/ofmeet',
            whitelisted_plugins: whitelistedPlugins
        };

        converse.initialize( config );

    }).catch(function (err) {
        console.error('access denied error', err);
    });
});

window.addEventListener('message', function (event)
{
    //console.log("inverse addListener message", event.data);

    if (event.data && event.data.action)
    {
        if (event.data.action == "pade.action.open.chat") openChat(event.data.from, event.data.name);
        if (event.data.action == "pade.action.open.chat.panel") openChatPanel(event.data.from);
        if (event.data.action == "pade.action.open.group.chat") openGroupChat(event.data.jid, event.data.label, event.data.nick, event.data.properties);
    }
});

function urlParam(name)
{
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    if (!results) { return undefined; }
    return unescape(results[1] || undefined);
};

function openChat(from, name)
{
    if (_inverse)
    {
        if (!name)
        {
            name = from.split("@")[0];
            if (name.indexOf("sms-") == 0) name = name.substring(4);
        }

        var contact = _converse.roster.findWhere({'jid': from});
        if (!contact) _inverse.roster.addAndSubscribe(from, name);
        _inverse.api.chats.open(from);
    }
}

function openChatPanel(from)
{
    if (_inverse) _inverse.api.chats.open(from);
}

function openGroupChat(jid, label, nick, properties)
{
    console.log("openGroupChat", jid, label, nick, properties);

    if (_inverse)
    {
        if (!properties) properties = {name: label, nick: nick};

        _inverse.api.rooms.open(jid, properties);

        if (properties.question)
        {
            setTimeout(function()
            {
                _inverse.connection.send(inverse.env.$msg({
                    to: jid,
                    from: _inverse.connection.jid,
                    type: "groupchat"
                }).c("subject", {
                    xmlns: "jabber:client"
                }).t(properties.question).tree());

            }, 1000);
        }
    }
}
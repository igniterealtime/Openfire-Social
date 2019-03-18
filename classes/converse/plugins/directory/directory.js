(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define(["converse"], factory);
    } else {
        factory(converse);
    }
}(this, function (converse) {
    var Strophe = converse.env.Strophe;
    var $iq = converse.env.$iq;
    var DirectoryDialog = null;
    var directoryDialog = null;
    var directoryAvailable = false;
    var _converse = null;

    converse.plugins.add("directory", {
        'dependencies': [],

        'initialize': function () {
            _converse = this._converse;

            DirectoryDialog = _converse.BootstrapModal.extend({
                initialize() {
                    _converse.BootstrapModal.prototype.initialize.apply(this, arguments);
                    this.model.on('change', this.render, this);
                },
                toHTML() {
                  return '<div class="modal" id="myModal"> <div class="modal-dialog modal-lg"> <div class="modal-content">' +
                         '<div class="modal-header"><h1 class="modal-title">Directory</h1><button type="button" class="close" data-dismiss="modal">&times;</button></div>' +
                         '<div class="modal-body">' +
                         '<input id="pade-directory-query" class="form-control" type="text" placeholder="Type a query string and press [Enter] to search user directory" ><p/><div id="pade-directory-results"></div>' +
                         '</div>' +
                         '<div class="modal-footer"> <button type="button" class="btn btn-danger" data-dismiss="modal">Close</button> </div>' +
                         '</div> </div> </div>';
                },
                afterRender() {
                  var that = this;
                  this.el.addEventListener('shown.bs.modal', function()
                  {
                      if (that.model.get("query"))
                      {
                          that.el.querySelector('#pade-directory-query').style.display = "none";
                          that.doDirectory();
                      }
                      else {
                        that.el.querySelector('#pade-directory-query').focus();
                      }

                  }, false);
                },
                events: {
                    'keyup #pade-directory-query': 'keyUp'
                },

                keyUp(ev) {
                    if (ev.key === "Enter")
                    {
                        var query = this.el.querySelector("#pade-directory-query").value.trim();
                        this.model.set("query", query)
                        this.doDirectory();
                    }
                },

                doDirectory() {
                    var that = this;
                    var query = that.model.get("query");

                    if (query != "")
                    {
                        var directoryResults = that.el.querySelector("#pade-directory-results");
                        console.log("doDirectory", query);

                        findUsers(query, function(userList)
                        {
                            displayUsers(userList, directoryResults);
                        });
                    }
                }
            });

            console.log("directory plugin is ready");
        },

        'overrides': {
            ChatBoxView: {

                renderToolbar: function renderToolbar(toolbar, options) {
                    var result = this.__super__.renderToolbar.apply(this, arguments);

                    var view = this;
                    var id = this.model.get("box_id");

                    addToolbarItem(view, id, "pade-directory-" + id, '<a title="Directory"><span class="fa fa-male"></span><span class="fa fa-female"></span></a>');
                    directoryAvailable = true;

                    setTimeout(function()
                    {
                        var directory = document.getElementById("pade-directory-" + id);

                        if (directory) directory.addEventListener('click', function(evt)
                        {
                            evt.stopPropagation();

                            directoryDialog = new DirectoryDialog({ 'model': new converse.env.Backbone.Model({}) });
                            directoryDialog.show();
                        }, false);
                    });

                    return result;
                }
            }
        }
    });

    var findUsers = function (search, callback)
    {
        console.debug('findUsers', search);

        var iq = $iq({type: 'set', to: "search." + _converse.connection.domain}).c('query', {xmlns: 'jabber:iq:search'}).c('x').t(search).up().c('email').t(search).up().c('nick').t(search);

        _converse.connection.sendIQ(iq, function(response)
        {
            var users = [];
            var items = response.querySelectorAll('query item');

            for (var i=0; i<items.length; i++)
            {
                console.debug('findUsers - item', items[i]);

                var jid = items[i].getAttribute('jid');
                var username = Strophe.getNodeFromJid(jid);
                var name = items[i].querySelector('nick').innerHTML;
                var email = items[i].querySelector('email').innerHTML;

                console.debug('findUsers response', username, name, jid, email);
                users.push({username: username, name: name, email: email, jid: jid});
            };

            if (callback) callback(users);

        }, function (error) {
            console.error('findUsers', error);
        });
    }

    var newElement = function (el, id, html)
    {
        var ele = document.createElement(el);
        if (id) ele.id = id;
        if (html) ele.innerHTML = html;
        document.body.appendChild(ele);
        return ele;
    }

    var addToolbarItem = function(view, id, label, html)
    {
        var placeHolder = view.el.querySelector('#place-holder');

        if (!placeHolder)
        {
            var smiley = view.el.querySelector('.toggle-smiley.dropup');
            smiley.insertAdjacentElement('afterEnd', newElement('li', 'place-holder'));
            placeHolder = view.el.querySelector('#place-holder');
        }
        placeHolder.insertAdjacentElement('afterEnd', newElement('li', label, html));
    }

    var removeDuplicates = function(userList)
    {
        console.debug('removeDuplicates', userList);

        var usersObj = {};

        for (var i=0; i < userList.length; i++ )
        {
            usersObj[userList[i].jid] = userList[i];
        }

        var users = new Array();

        for (var key in usersObj)
        {
            users.push(usersObj[key]);
        }

        return users;
    }

    var displayUsers = function(userList, directoryResults)
    {
        console.debug('displayUsers', userList);

        var users = removeDuplicates(userList);
        var html = "<div id='plugin-directory'><table style='margin-left: 15px'><tr><th>Name</th><th>JID</th><th>Email</th></tr>";
        var count = 0;

        for (var i=0; i<users.length; i++)
        {
            var user = users[i];
            if (!user.email) user.email = "";

            html = html + "<tr><td><a class='plugin-directory-jid' title='" + user.name + "' id='" + user.jid + "' href='#'>" + user.name + "</a></td><td>" + user.jid + "</td><td>" + user.email + "</td></tr>";
        }
        html = html + "</table></div>"

        if (users.length == 0) html = "No user found";

        directoryResults.innerHTML = "<p/><p/>" + html;

        setTimeout(function()
        {
            var jids = directoryResults.querySelectorAll(".plugin-directory-jid");

            for (var i=0; i<jids.length; i++)
            {
                console.debug('displayUsers - jids', jids[i]);

                jids[i].addEventListener("click", function(e)
                {
                    e.stopPropagation();
                    var user = e.target;

                    console.debug("findUsers - click", user.id, user.name, user.title);
                    _converse.api.chats.open(user.id)
                });
            }
        }, 1000);
    }

}));

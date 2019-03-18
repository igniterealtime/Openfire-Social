(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define(["converse"], factory);
    } else {
        factory(converse);
    }
}(this, function (converse) {
    var VmsgDialog = null;
    var vmsgDialog = null;
    var _converse = null;

    converse.plugins.add("vmsg", {
        'dependencies': [],

        'initialize': function () {
            _converse = this._converse;

            VmsgDialog = _converse.BootstrapModal.extend({
                initialize() {
                    _converse.BootstrapModal.prototype.initialize.apply(this, arguments);
                },
                toHTML() {
                  var view = this.model.get("view");
                  var id = view.model.get("id").split("@")[0];

                  return '<div class="modal" id="myModal"> <div class="modal-dialog"> <div class="modal-content">' +
                         '<div class="modal-header">' +
                         '  <h1 class="modal-title">Voice Message</h1>' +
                         '  <button type="button" class="close" data-dismiss="modal">&times;</button>' +
                         '</div>' +
                         '<div class="modal-body"><iframe id="iframe-vmsg-' + id + '" src="converse/plugins/vmsg/index.html" style="width:100%; height:300px; border:none; margin:0; padding:0; overflow:hidden;"></iframe></div>' +
                         '<div class="modal-footer"> <button type="button" class="btn btn-success btn-upload-vmsg" data-dismiss="modal">Upload</button> <button type="button" class="btn btn-danger" data-dismiss="modal">Close</button> </div>' +
                         '</div> </div> </div>';
                },
                events: {
                    "click .btn-upload-vmsg": "uploadVmsg",
                },

                uploadVmsg() {
                    var view = this.model.get("view");
                    var id = view.model.get("id").split("@")[0];
                    var mp3File = this.el.querySelector("#iframe-vmsg-" + id).contentWindow.getMp3File();

                    console.log("upload vmsg", mp3File, id);

                    if (!mp3File)
                    {
                        alert("Nothing to upload!!");
                        return;
                    }
                    view.model.sendFiles([mp3File]);
                }
            });

            console.log("vmsg plugin is ready");
        },

        'overrides': {
            ChatBoxView: {

                renderToolbar: function renderToolbar(toolbar, options) {
                    var result = this.__super__.renderToolbar.apply(this, arguments);

                    var view = this;
                    var id = this.model.get("box_id");

                    addToolbarItem(view, id, "pade-vmsg-" + id, '<a class="fas fa-microphone" title="Voice Message. Click to create"></a>');

                    setTimeout(function()
                    {
                        var vmsg = document.getElementById("pade-vmsg-" + id);

                        vmsg.addEventListener('click', function(evt)
                        {
                            evt.stopPropagation();

                            vmsgDialog = new VmsgDialog({ 'model': new converse.env.Backbone.Model({view: view}) });
                            vmsgDialog.show();

                        }, false);
                    });

                    return result;
                }
            }
        }
    });

    function newElement(el, id, html)
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
}));

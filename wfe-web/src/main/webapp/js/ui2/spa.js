var wfe = new function() {

    this.wait = function() {
        $('#spa-body').hide();
        $('#spa-error').hide();
        $('#spa-wait').show();
    };
    this.ready = function() {
        $('#spa-error').hide();
        $('#spa-wait').hide();
        $('#spa-body').show();
    };
    this.error = function(msg) {
        $('#spa-body').hide();
        $('#spa-wait').hide();
        $('#spa-error-msg').text(msg ? msg : "Ошибка при загрузке данных.");
        $('#spa-error').show();
    };

    this.ajaxGetJsonAndReady = function(url, data, onSuccess) {
        wfe.wait();
        $.ajax({
            url: "/wfe/api/" + url,
            dataType: "json",
            data: data,
            success: function(json) {
                if (json.error) {
                    wfe.error(json.error);
                } else {
                    onSuccess(json.data);
                    wfe.ready();
                }
            },
            error: function() {
                wfe.error();
            }
        });
    }
};

/**
 * @author Dmitry Grigoriev (dimgel)
 */
wfe.spa = new function() {
    var self = this;
    var whenStartedString;
    var defaultUrl = "/myTasks";
    var titleSuffix = document.title;
    var attachedScriptsAndStyles = {};
    var cachedHtmls = {};
    var currentJsController = null;

    function showPage(html, fromCache) {
        // Prevent loss of <html>, <head>, <body> tags; https://stackoverflow.com/a/10585079/4247442
        var eHtml = document.createElement("html");
        eHtml.innerHTML = html;
        var qHtml = $(eHtml);

        var title = qHtml.find("head title").text();
        document.title = title ? title + " - " + titleSuffix : titleSuffix;

        var qBody = qHtml.find("body");
        $("#spa-body").empty().append(qBody.contents().clone());

        // HTML can include JS & CSS files in <head>, inline JS in body, and <body data-jsController="..."> attribute.
        // JS files cannot contain $(function) since they are loaded only once.
        function onPageLoaded() {
            if (currentJsController && currentJsController.onUnload) {
                currentJsController.onUnload();
            }
            var s = qBody.attr('data-jsController');
            currentJsController = s ? eval(s) : null;
            if (currentJsController && currentJsController.onLoad) {
                // This must call wfe.wait() if it performs ajax requests; and always -- wfe.ready() or wfe.error().
                currentJsController.onLoad();
            } else {
                wfe.ready();
            }
        }

        // Page must not and can not call gotoPage() before it's fully loaded.
        // So if we're loading from cache, all JS and CSS files must already be loaded.
        var numFilesToWaitFor = 0;
        if (!fromCache) {
            function onFileLoaded() {
                if (--numFilesToWaitFor <= 0) {
                    onPageLoaded();
                }
            }

            // Inline scripts & styles are ignored in <head>.
            qHtml.find("head script[type='text/javascript'][src], head link[rel='stylesheet'][type='text/css'][href]").each(function () {
                var isScript = this.tagName === 'SCRIPT';
                var attrName = isScript ? 'src' : 'href';
                var fileUrl = this.getAttribute(attrName);
                if (!attachedScriptsAndStyles[fileUrl]) {
                    attachedScriptsAndStyles[fileUrl] = true;
                    numFilesToWaitFor++;

                    // Does not work with jquery; https://stackoverflow.com/a/22534608/4247442, https://stackoverflow.com/a/11425185/4247442
                    var e = document.createElement(this.tagName);
                    e.onload = onFileLoaded;
                    if (isScript) {
                        e.setAttribute('type', 'text/javascript');
                    } else {
                        e.setAttribute('rel', 'stylesheet');
                        e.setAttribute('type', 'text/css');
                    }
                    e.setAttribute(attrName, fileUrl + "?" + whenStartedString);
                    document.head.appendChild(e);
                }
            });
        }
        if (numFilesToWaitFor === 0) {
            onPageLoaded();
        }
    }

    function onHashChange() {
        var hash = window.location.hash;
        if (!hash.startsWith("#/")) {
            self.gotoUrl(defaultUrl);
            return;
        }

        var url = "/wfe/ui2" + hash.substr(1) + "?.=" + whenStartedString;

        if (cachedHtmls[url]) {
            showPage(cachedHtmls[url], true);
        } else {
            wfe.wait();
            $.ajax({
                url: url,
                dataType: "html",
                success: function (html) {
                    cachedHtmls[url] = html;
                    showPage(html, false);
                },
                error: function() {
                    wfe.error();
                }
            });
        }
    }

    this.gotoUrl = function(s) {
        s = "#" + s;
        if (window.location.hash === s) {
            // Reload current page (otherwise F5 & Ctrl+F5 won't work, also we may need to reload programmatically).
            onHashChange();
        } else {
            window.location.hash = s;
        }
    };

    this.onLoad = function(wss) {
        whenStartedString = wss;
        $(window).on("hashchange", onHashChange);
        onHashChange();
    }
};

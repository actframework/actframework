if (typeof String.prototype.contains != 'function') {
    String.prototype.contains = function (str) {
        return this.indexOf(str) != -1;
    };
}

function __processCsrfSetup(setup, submitMethod) {
    try {
        if (jQuery.csrfConf && jQuery.csrfConf.cookieName && jQuery.csrfConf.headerName && "post" === submitMethod) {
            var token = jQuery.csrfConf.token
            if (token) {
                setup.beforeSend = function(xhr) {
                    xhr.setRequestHeader(jQuery.csrfConf.headerName, token)
                }
            }
        }
    } catch (e) {
        console.warn(e)
    }
}

function __processCsrfForm() {
    var csrfToken = jQuery.csrfConf.token
    if (csrfToken) {
        jQuery('form:not(.no-csrf)').each(function() {
            var $form = $(this)
            if ($form.children('input[name=' + jQuery.csrfConf.paramName + ']').length == 0) {
                $form.append('<input type="hidden" name="' + jQuery.csrfConf.paramName + '" value="' + csrfToken + '">')
            }
        })
    }
}

jQuery.getJSON('/~/conf/csrf', function(csrfConf) {
    jQuery.csrfConf = csrfConf;
    jQuery.csrfConf.token = jQuery.cookie(csrfConf.cookieName)
    __processCsrfForm()
})

if (!jQuery.cookie) {
    jQuery.cookie = function(name) {
        var nameEQ = name + "=";
        var ca = document.cookie.split(';');
        for(var i=0;i < ca.length;i++) {
            var c = ca[i];
            while (c.charAt(0)==' ') c = c.substring(1,c.length);
            if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
        }
        return false;
    }
}

jQuery.createWebSocket = function(path, errorCallback) {
    if (!path.startsWith('/')) {
        var pathname = window.location.pathname
        if (pathname.endsWith('/')) {
            path = pathname + path
        } else {
            path = pathname + '/' + path
        }
    }
    try {
        return new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") + window.location.host + path);
    } catch (e) {
        if (jQuery.isFunction(errorCallback)) {
            errorCallback.call(e)
        }
        throw e
    }
}

var checkAjaxRedirect = function(data, testStatus, jqXHR) {
   if (data && data.status === 278) {
       window.location = data.getResponseHeader("Location");
   }
}

jQuery.each(["get", "post", "put", "delete", "patch" ], function (i, method) {
    jQuery[ method ] = function (url, data, callback, type, errorCallback) {
        // shift arguments if data argument was omitted
        if (jQuery.isFunction(data)) {
            errorCallback = errorCallback || type;
            type = type || callback;
            callback = data;
            data = undefined;
        }

        if (typeof errorCallback === 'undefined') {
            if (jQuery.isFunction(type)) {
                errorCallback = type
            }
        }

        var submitMethod = method;

        if (method === "put" || method === "patch" || method === "delete") {
            var hasParam = url.contains("?");
            if (!hasParam) url = url + "?_method=" + method;
            submitMethod = "post";
        }

        var setup = {
            url: url,
            type: submitMethod,
            dataType: type,
            data: data,
            success: callback,
            error: errorCallback
        }

        __processCsrfSetup(setup, submitMethod)
        return jQuery.ajax(setup).always(checkAjaxRedirect);
    };
});

jQuery.each(["getJSON", "postJSON", "putJSON", "deleteJSON", "patchJSON"], function (i, method) {
    jQuery[ method ] = function (url, data, callback, errorCallback) {
        if (jQuery.isFunction(data)) {
            errorCallback = callback
            callback = data;
            data = undefined;
        }

        var submitMethod = method;

        if (method.startsWith("put") || method.startsWith("patch") || method.startsWith("delete")) {
            var hasParam = url.contains("?");
            if (!hasParam) url = url + "?_method=" + method.replace("JSON", "");
            submitMethod = "post";
        }
//        if (method.startsWith("get")) {
//            var hasParam = url.contains("?");
//            if (!hasParam) {
//                url = url +"?_now="+ new Date().getTime();
//            } else {
//                url = url +"&_now="+ new Date().getTime();
//            }
//        }
        var setup = {
            url: url,
            type: submitMethod.replace("JSON", ""),
            dataType: "json",
            data: data,
            success: callback,
            error: errorCallback
        }
        __processCsrfSetup(setup, submitMethod)
        return jQuery.ajax(setup).always(checkAjaxRedirect);
    };
});

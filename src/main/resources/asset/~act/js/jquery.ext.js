if (typeof String.prototype.contains != 'function') {
    String.prototype.contains = function (str) {
        return this.indexOf(str) != -1;
    };
}

jQuery.createWebSocket = function(path) {
    if (!path.startsWith('/')) {
        var pathname = window.location.pathname
        if (pathname.endsWith('/')) {
            path = pathname + path
        } else {
            path = pathname + '/' + path
        }
    }
    return new WebSocket(((window.location.protocol === "https:") ? "wss://" : "ws://") + window.location.host + path);
}

var checkAjaxRedirect = function(data, testStatus, jqXHR) {
   if (data && data.status === 278) {
       window.location = data.getResponseHeader("Location");
   }
}

jQuery.each(["get", "post", "put", "delete", "patch" ], function (i, method) {
    jQuery[ method ] = function (url, data, callback, type) {
        // shift arguments if data argument was omitted
        if (jQuery.isFunction(data)) {
            type = type || callback;
            callback = data;
            data = undefined;
        }

        var submitMethod = method;

        if (method === "put" || method === "patch" || method === "delete") {
            var hasParam = url.contains("?");
            if (!hasParam) url = url + "?_method=" + method;
            submitMethod = "post";
        }

        return jQuery.ajax({
            url: url,
            type: submitMethod,
            dataType: type,
            data: data,
            success: callback
        }).always(checkAjaxRedirect);
    };
});

jQuery.each(["getJSON", "postJSON", "putJSON", "deleteJSON", "patchJSON"], function (i, method) {
    jQuery[ method ] = function (url, data, callback) {
        if (jQuery.isFunction(data)) {
            callback = data;
            data = undefined;
        }

        var submitMethod = method;

        if (method.startsWith("put") || method.startsWith("patch") || method.startsWith("delete")) {
            var hasParam = url.contains("?");
            if (!hasParam) url = url + "?_method=" + method.replace("JSON", "");
            submitMethod = "post";
        }
        if (method.startsWith("get")) {
            var hasParam = url.contains("?");
            if (!hasParam) {
                url = url +"?now="+ new Date().getTime();
            } else {
                url = url +"&now="+ new Date().getTime();
            }
        }
        return jQuery.ajax({
            url: url,
            type: submitMethod.replace("JSON", ""),
            dataType: "json",
            data: data,
            success: callback
        }).always(checkAjaxRedirect);
    };
});

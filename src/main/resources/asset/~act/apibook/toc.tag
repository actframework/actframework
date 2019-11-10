<toc>
    <div id="toc-container">
        <div class="toc-inner">
            <ul class="toc-list">
                <li class="toc" show={showList(GET)}>GET</li>
                <virtual each="{endpoint in GET}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">
                            <raw html={ endpoint.pathHtml }></raw>
                        </a>
                    </li>
                </virtual>
                <li class="toc" show={showList(POST)}>POST</li>
                <virtual each="{endpoint in POST}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">
                            <raw html={ endpoint.pathHtml }></raw>
                        </a>
                    </li>
                </virtual>
                <li class="toc" show={showList(PUT)}>PUT</li>
                <virtual each="{endpoint in PUT}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">
                            <raw html={ endpoint.pathHtml }></raw>
                        </a>
                    </li>
                </virtual>
                <li class="toc" show={showList(DELETE)}>DELETE</li>
                <virtual each="{endpoint in DELETE}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">
                            <raw html={ endpoint.pathHtml }></raw>
                        </a>
                    </li>
                </virtual>
                <li class="toc" show={showList(PATCH)}>PATCH</li>
                <virtual each="{endpoint in PATCH}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">
                            <raw html={ endpoint.pathHtml }></raw>
                        </a>
                    </li>
                </virtual>
            </ul>
        </div>
    </div>
    <style>
        @media print {
            #toc-container {
                display: none;
            }
        }
        #toc-container {
            width: 30%;
            position: fixed;
            top: 76px;
            right: 0;
            bottom: 12px;
            overflow-y: auto;
            overflow-x: hidden;
            color: #bbbbbb;
        }
        .toc-inner {
            overflow-y: visible;
            padding: 4px 0 0 10px;
        }
        .toc-inner ul.toc-list {
            list-style-type: none;
            margin: 0;
            padding: 0 8px 0 0;
        }
        .toc-inner ul.toc-list li {
            box-sizing: border-box;
            padding: 3px 0 3px 12px;
            position: relative;
            transition: all .3s ease-in-out;
        }
        .toc-inner ul.toc-list li.toc {
            font-size: 128%;
            padding-top: 22px;
            font-weight: 800;
        }
        .toc-inner ul.toc-list li.toc:first-child {
            padding-top: 0;
        }
        .toc-inner ul.toc-list:not(.embedded) li:before {
            border-left: 1px solid #dbdbdb;
            bottom: 0;
            content: "";
            left: 0;
            position: absolute;
            top: 0;
        }
        .toc-inner ul.toc-list li a {
            color: #bbb;
            text-decoration: none;
            display: table-cell;
        }
        span.var {
            color: #fff;
        }
    </style>
    <script>
        var self = this
        self.GET = []
        self.POST = []
        self.PUT = []
        self.DELETE = []
        self.selectedModules = []
        self.buildTree = function(endpoints) {
            for (var i = 0, j = endpoints.length; i < j; ++i) {
                var endpoint = endpoints[i]
                var handlers = self[endpoint.httpMethod]
                if (!handlers) {
                    continue
                }
                handlers.push(endpoint)
                handlers.sort(function(a, b) {
                    if (a.path < b.path) return -1
                    if (a.path > b.path) return 1
                    return 0
                })
            }
        }
        showList(endpoints) {
            if (endpoints.length == 0) {
                return false
            }
            for (var i = 0, j = endpoints.length; i < j; ++i) {
                if (self.show(endpoints[i])) {
                    return true
                }
            }
            return false
        }
        riot.store.on('endpoints-fetched', function(endpoints) {
            self.buildTree(endpoints)
            self.update()
        })
        riot.store.on('module-selected', function(modules) {
            self.selectedModules = modules;
            self.update()
        })
        riot.store.on('filter-changed', function(filter) {
            self.filter = filter
            self.update()
        })
        show(endpoint) {
            var show = self.selectedModules.indexOf(endpoint.module) > -1
            if (!show) {
                return false
            }
            if (!self.filter) {
                return show
            }
            x = endpoint
            return x.description.toLowerCase().includes(self.filter) || x.id.toLowerCase().includes(self.filter) || x.path.toLowerCase().includes(self.filter)
        }
    </script>
</toc>
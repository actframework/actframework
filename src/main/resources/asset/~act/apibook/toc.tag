<toc>
    <div id="toc-container">
        <div class="toc-inner">
            <ul class="toc-list">
                <li class="toc">GET</li>
                <virtual each="{endpoint in GET}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">{endpoint.path}</a>
                    </li>
                </virtual>
                <li class="toc">POST</li>
                <virtual each="{endpoint in POST}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">{endpoint.path}</a>
                    </li>
                </virtual>
                <li class="toc">PUT</li>
                <virtual each="{endpoint in PUT}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">{endpoint.path}</a>
                    </li>
                </virtual>
                <li class="toc">DELETE</li>
                <virtual each="{endpoint in DELETE}">
                    <li show={show(endpoint)}>
                        <a href="#{endpoint.xid}" title="{endpoint.description}">{endpoint.path}</a>
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
            width: 24%;
            position: fixed;
            top: 76px;
            right: 0;
            bottom: 12px;
            overflow-y: auto;
            overflow-x: hidden;
            color: #bbbbbb;
        }
        .toc-inner {
            font-size: 13px;
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
            font-size: 16px;
            line-height: 24px;
            padding: 3px 0 3px 12px;
            position: relative;
            transition: all .3s ease-in-out;
        }
        .toc-inner ul.toc-list li.toc {
            font-size: 128%;
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
            color: #999;
            text-decoration: none;
            display: table-cell;
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
        riot.store.on('endpoints-fetched', function(endpoints) {
            self.buildTree(endpoints)
            self.update()
        })
        riot.store.on('module-selected', function(modules) {
            self.selectedModules = modules;
            self.update()
        })
        show(endpoint) {
            var show = self.selectedModules.indexOf(endpoint.module) > -1
            return show
        }
    </script>
</toc>
<endpoint-list>
    <div class="endpoint" each={ endpoint in endpoints } show={ show(endpoint) }>
        <a class="id" id="{ endpoint.xid }">&nbsp;</a>
        <h4 class="entry">[{ endpoint.httpMethod }] { endpoint.path }</h4>
        <div class="desc"><raw html={ endpoint.richDesc }></raw></div>
        <div class="param-list">
            <h5>Parameters</h5>
            <div class="param-list-body">
                <div if={ endpoint.params.length== 0 }>
                    N/A
                </div>
                <table if={ endpoint.params.length> 0 }>
                    <thead>
                    <tr>
                        <th>name</th>
                        <th>type</th>
                        <th>required</th>
                        <th>default</th>
                        <th>description</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr each={ endpoint.params }>
                        <td title={ tooltip }>{ name }</td>
                        <td>{ type }</td>
                        <td>{ required }</td>
                        <td>
                            <span if={ defaultValue }>{ defaultValue }</span>
                            <span if={ !defaultValue }>N/A</span>
                        </td>
                        <td><raw html={ richDesc }></raw></td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <!-- eof param list -->
        <div class="query-sample" if="{ endpoint.sampleQuery }">
            <h5>Query example</h5>
            <pre class="code">{ endpoint.sampleQuery }</pre>
        </div>
        <div class="post-sample" if="{ endpoint.sampleJsonPost }">
            <h5>Json body example</h5>
            <raw html={ endpoint.sampleJsonPost}>
            <pre class="code"><code class="json">{ endpoint.sampleJsonPost }</code></pre>
        </div>
        <div class="return" if="{ endpoint.returnDescription }">
            <h5>Return</h5>
            <div style="margin-top:10px"><raw html={ endpoint.returnDescription}></div>
        </div>
        <div class="return-sample" if="{ endpoint.returnSample }">
            <h5 if="{ !endpoint.returnDescription }">Return value sample</h5>
            <raw html={endpoint.returnSample}>
        </div>
    </div>
    <div id='bottom-padding'>&nbsp;</div>
    <style>
        a.id {
            display:block;
            margin-bottom: 50px;
        }

        h3, h4, h5 {
            margin-bottom: 0.2em;
        }

        h4.entry {
            font-family: "Noto Sans Mono CJK SC", Menlo, Consolas, "Envy Code R";
            border-bottom: 1px solid #aaa;
            display: block;
            padding-bottom: 10px;
        }

        div.desc {
            margin-top: 10px;
            margin-bottom: 10px;
        }

        a[id] {
            display: block;
        }

        .desc ul li {
            line-height: 160%;
            font-size: 13px;
        }

        table {
            font-family: Roboto, 'San Francisco Display', 'Noto Sans', 'Segoe UI', 'Arial', 'Sans';
            border-collapse: collapse;
            text-align: left;
            width: 100%;
        }

        thead, tbody {
            margin: 5px 10px;
        }

        td, th {
            width: 1px;
            padding: 5px 1em;
            font-size: 12px;
            white-space: nowrap;
        }

        th {
            font-weight: 500;
            border-bottom: 1px solid;
            text-transform: uppercase;
            font-size: .75em;
        }

        td {
            font-weight: 500;
            padding-bottom: 0;
            vertical-align: top;
        }

        td:not(:first-child) {
            font-family: "Envy Code R", "Roboto Mono", Menlo, Consolas, Monaco, "Lucida Console", "Liberation Mono", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Courier New", monospace;
        }

        td:first-child, th:first-child {
            font-weight: 500;
            font-size: .8em;
            border-right: 1px solid;
            text-align: right;
        }

        td:last-child, th:last-child {
            width: 100%;
        }

        td pre {
            margin-top: 0;
            margin-bottom: 0;
        }

        div.endpoint {
            padding-bottom: 25px;
        }

        .endpoint > .desc {
            font-weight: 500;
            max-width: 1280px;
        }

        .endpoint > .desc h1, .endpoint > .desc h2, .endpoint > .desc h3, .endpoint > .desc h4 {
            font-weight: bold;
        }

        .endpoint > .desc h1 {
            font-size: 20px;
        }
        .endpoint > .desc h2 {
            font-size: 18px;
        }
        .endpoint > .desc h3 {
            font-size: 16px;
        }
        .endpoint > .desc h4, .endpoint > .desc h5 {
            font-size: 14px;
        }

        .endpoint > .desc code {
            font-size: 14px;
        }

        pre.code {
            padding: 10pt;
            background: #444;
            max-width: 1280px;
            font-size: 14px;
            font-weight: 400;
            line-height: 1.5;
            overflow-x: auto;
            margin: 16px 0 16px 0;
        }

        .param-list-body, .return-desc {
            padding: 10px;
            background: #444;
            max-width: 1280px;
            margin: 16px 0 16px 0;
        }

        #bottom-padding {
            padding-bottom: 1024px;
        }

        @media print {
            pre.code {
                font-family: "Envy Code R", Consolas, Menlo, Monaco, "Lucida Console", "Liberation Mono", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Courier New", monospace;
                max-width: 1600px;
            }
            .endpoint > pre.code {
                border: 1px solid black;
            }
            .return-sample pre.code,
            .post-sample pre.code {
                font-size: 9px;
            }
        }
    </style>
    <script>
        var self = this
        self.filter = false
        self.endpoints = []
        self.sysInfo = {}
        self.on('mount', function () {
            self.fetchEndpoints()
        })
        self.selectedModules = []
        patchPreCode() {
            $('pre > code').each(function() {
                $(this).parent().addClass('code')
            })
        }
        filteredEndpoints() {
            if (!self.filter) {
                return self.endpoints
            } else {
                return self.endpoints.filter(x => x.description.toLowerCase().includes(self.filter) || x.id.toLowerCase().includes(self.filter) || x.path.toLowerCase().includes(self.filter))
            }
        }
        highlightjs(s) {
            if (!s) return ''
            s = "```json\n" + s + "\n```"
            return riot.md.render(s)
        }
        fetchEndpoints() {
            $.getJSON('/~/apibook/endpoints', function(endpoints) {
                for(var i = 0, j = endpoints.length; i < j; ++i) {
                    var endpoint = endpoints[i]
                    endpoint.richDesc = riot.md.render(endpoint.description);
                    if (endpoint.returnDescription) {
                        var html = riot.md.render(endpoint.returnDescription)
                        var tag = $(html)
                        endpoint.richReturnDescription = tag.html()
                    } else {
                        endpoint.richReturnDescription = ''
                    }
                    endpoint.sampleJsonPost = self.highlightjs(endpoint.sampleJsonPost)
                    endpoint.returnSample = self.highlightjs(endpoint.returnSample)
                    for (var pi = 0, pj = endpoint.params.length; pi < pj; ++pi) {
                        var param = endpoint.params[pi]
                        if (param && param.description) {
                            var html = riot.md.render(param.description)
                            var tag = $(html)
                            param.richDesc = tag.html()
                            if (!param.richDesc || typeof param.richDesc === 'undefined') {
                                param.richDesc = ''
                            }
                        } else if (param) {
                            param.richDesc = ''
                        }
                    }
                }
                self.endpoints = endpoints
                self.update()
                riot.store.trigger('endpoints-fetched', endpoints);
                self.patchPreCode()
                if(window.location.hash) {
                    var anchor = document.getElementById(window.location.hash.substr(1))
                    if (anchor) {
                         anchor.scrollIntoView();
                    }
                }
            })
        }
        riot.store.on('module-selected', function(modules) {
            self.selectedModules = modules;
            self.update()
            self.patchPreCode()
        })
        riot.store.on('filter-changed', function(filter) {
            self.filter = filter
            self.update()
            self.patchPreCode()
        })
        show(endpoint) {
            var ret = self.selectedModules.indexOf(endpoint.module) > -1;
            if (!ret) {
                return false
            }
            if (!self.filter) {
                return ret
            }
            x = endpoint
            return x.description.toLowerCase().includes(self.filter) || x.id.toLowerCase().includes(self.filter) || x.path.toLowerCase().includes(self.filter)
        }
    </script>
</endpoint-list>
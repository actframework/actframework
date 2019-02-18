<endpoint-list>
    <div class="endpoint" each={ endpoint in endpoints } show={ show(endpoint) }>
        <a class="id" id="{ endpoint.xid }">&nbsp;</a>
        <pre class="code">[{ endpoint.httpMethod }] { endpoint.path }</pre>
        <div class="desc"><raw html={ endpoint.richDesc }></raw></div>
        <div class="param-list">
            <h4>Parameters</h4>
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
                        <td>{ name }</td>
                        <td>{ type }</td>
                        <td>{ required }</td>
                        <td>
                            <span if={ defaultValue }>{ defaultValue }</span>
                            <span if={ !defaultValue }>N/A</span>
                        </td>
                        <td><pre>{ description }</pre></td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <!-- eof param list -->
        <div class="query-sample" if="{ endpoint.sampleQuery }">
            <h4>Query example</h4>
            <pre class="code">{ endpoint.sampleQuery }</pre>
        </div>
        <div class="post-sample" if="{ endpoint.sampleJsonPost }">
            <h4>Json body example</h4>
            <pre class="code">{ endpoint.sampleJsonPost }</pre>
        </div>
        <div class="return-sample" if="{ endpoint.returnSample }">
            <h4>Return value sample</h4>
            <pre class="code">{ endpoint.returnSample }</pre>
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

        div.desc {
            margin-top: 10px;
            margin-bottom: 10px;
        }

        a[id] {
            display: block;
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
            font-size: .9em;
            white-space: nowrap;
        }

        th {
            font-weight: 500;
            border-bottom: 1px solid;
            text-transform: uppercase;
            font-size: .75em;
        }

        td {
            font-weight: 300;
            padding-bottom: 0;
            vertical-align: top;
        }

        td:not(:first-child) {
            font-family: monospace;
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
        }

        div.endpoint {
            padding-bottom: 25px;
            border-bottom: 1px solid #aaa;
        }

        .endpoint > .desc {
            font-weight: 300;
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
            font-size: 12pt;
            line-height: 1.5;
            overflow-x: auto;
            margin: 16px 0 16px 0;
        }

        .param-list-body {
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
        self.endpoints = []
        self.sysInfo = {}
        self.on('mount', function () {
            self.fetchEndpoints()
        })
        self.selectedModules = []
        fetchEndpoints() {
            $.getJSON('/~/apibook/endpoints', function(endpoints) {
                for(var i = 0, j = endpoints.length; i < j; ++i) {
                    var endpoint = endpoints[i];
                    endpoint.richDesc = riot.md.render(endpoint.description);
                }
                self.endpoints = endpoints
                self.update()
                riot.store.trigger('endpoints-fetched', endpoints);
            })
        }
        riot.store.on('module-selected', function(modules) {
            self.selectedModules = modules;
            self.update()
        })
        show(endpoint) {
            return self.selectedModules.indexOf(endpoint.module) > -1;
        }
    </script>
</endpoint-list>
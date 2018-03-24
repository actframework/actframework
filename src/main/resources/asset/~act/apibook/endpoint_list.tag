<endpoint-list>
    <div class="endpoint" each={ endpoints }>
        <a class="id" id="{ xid }">&nbsp;</a>
        <pre class="code">[{ httpMethod }] { path }</pre>
        <div class="desc">{ description }</div>
        <div class="param-list">
            <h4>Parameters</h4>
            <div class="param-list-body">
                <div if={ params.length== 0 }>
                    N/A
                </div>
                <table if={ params.length> 0 }>
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
                    <tr each={ params }>
                        <td>{ name }</td>
                        <td>{ type }</td>
                        <td>{ required }</td>
                        <td>
                            <span if={ defaultValue }>{ defaultValue }</span>
                            <span if={ !defaultValue }>N/A</span>
                        </td>
                        <td>{ description }</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <!-- eof param list -->
        <div class="query-sample" if="{ sampleQuery }">
            <h4>Query example</h4>
            <pre class="code">{ sampleQuery }</pre>
        </div>
        <div class="post-sample" if="{ sampleJsonPost }">
            <h4>Json body example</h4>
            <pre class="code">{ sampleJsonPost }</pre>
        </div>
        <div class="return-sample" if="{ returnSample }">
            <h4>Return value sample</h4>
            <pre class="code">{ returnSample }</pre>
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

        div.endpoint {
            padding-bottom: 25px;
            border-bottom: 1px solid #aaa;
        }

        .endpoint > .desc {
            font-weight: 300;
        }

        pre.code {
            padding: 10pt;
            background: #444;
            max-width: 800px;
            font-size: 12pt;
            line-height: 1.5;
            overflow-x: auto;
            margin: 16px 0 16px 0;
        }

        .param-list-body {
            padding: 10px;
            background: #444;
            max-width: 800px;
            margin: 16px 0 16px 0;
        }

        #bottom-padding {
            padding-bottom: 1024px;
        }
    </style>
    <script>
        var self = this
        self.endpoints = []
        self.sysInfo = {}
        self.on('mount', function () {
            self.fetchEndpoints()
        })
        fetchEndpoints() {
            $.getJSON('/~/apibook/endpoint', function(endpoints) {
                self.endpoints = endpoints
                self.update()
                riot.store.trigger('endpoints-fetched', endpoints);
            })
        }
    </script>
</endpoint-list>
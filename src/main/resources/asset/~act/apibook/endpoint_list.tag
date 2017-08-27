<endpoint-list>
    <div class="endpoint" each={ endpoints }>
        <h3>{ description }</h3>
        <pre class="code">[{ method }] { path }</pre>
        <br/>
        <div class="param-list">
            <h4>Parameters</h4>
            <div if={ params.length== 0 }>
                N/A
            </div>
            <table if={ params.length> 0 }>
                <thead>
                <tr>
                    <th>name</th>
                    <th>type</th>
                    <th>default</th>
                    <th>description</th>
                </tr>
                </thead>
                <tbody>
                <tr each={ params }>
                    <td>{ name }</td>
                    <td>
                        { type }
                    </td>
                    <td>
                        <span if={ defaultValue }>{ defaultValue }</span>
                        <span if={ !defaultValue }>N/A</span>
                    </td>
                    <td>{ description }</td>
                </tr>
                </tbody>
            </table>
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
    <script>
        var self = this
        self.endpoints = []
        self.on('mount', function () {
            self.endpoints = self.fetchEndpoints()
            self.update()
        })
        fetchEndpoints()
        {
            $.getJSON('/~/apidoc/endpoint', function (endpoints) {
                self.endpoints = endpoints
                self.update()
            })
        }
    </script>
</endpoint-list>
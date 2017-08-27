<endpoint-list>
    <div each={ endpoints }>
        <h3>{ description }</h3>
        <div class="code">
            [{ method }] { path }
        </div>
        <div if={ params.length == 0 }>
            <br/>
            No parameter required
        </div>
        <virtual if={ params.length > 0 }>
            <br/>
            <div>Parameters</div>
            <table>
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
                    <td>{ type }</td>
                    <td>
                        <span if={ defaultValue }>{ defaultValue }</span>
                        <span if={ !defaultValue }>N/A</span>
                    </td>
                    <td>{ description }</td>
                </tr>
                </tbody>
            </table>
        </virtual>
    </div>
    <script>
        var self = this
        self.endpoints = []
        self.on('mount', function() {
            self.endpoints = self.fetchEndpoints()
            self.update()
        })
        fetchEndpoints() {
            $.getJSON('/~/apidoc/endpoint', function(endpoints) {
                self.endpoints = endpoints
                self.update()
            })
        }
    </script>
</endpoint-list>
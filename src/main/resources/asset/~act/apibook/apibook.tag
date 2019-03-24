<apibook>
    <style>
        #search-box-container {
            position: absolute;
            top: 26px;
            right: 20px;
            width: 150px;
            padding: 5px;
        }
    </style>
    <div class="header">
        <h1>API Book - { sysInfo.appName }</h1>
        <input type="text" id="search-box-container" placeholder="filter" oninput={filterUpdate}>
    </div>
    <div class="content">
        <module-list></module-list>
        <endpoint-list></endpoint-list>
        <toc></toc>
    </div>
    <style>
        .header {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            background: #222;
            z-index: 999;
        }
        .header h1 {
            margin: 10px;
            padding-left: 8px;
        }
        .content {
            position: relative;
            display: block;
            padding-left: 10px;
        }
        endpoint-list {
            width: 55%;
            left: 15%;
            overflow-x: hidden;
            position: absolute;
        }
        @media print {
            .header {display: none;}
        }
    </style>
    <script>
        var self = this
        self.throttle = false
        self.sysInfo = {}
        self.on('mount', function () {
            self.fetchSysInfo()
        })
        fetchSysInfo() {
            $.getJSON('/~/info', function(info) {
                self.sysInfo = info;
                document.title = 'API book - ' + info.appName;
                self.update();
            })
        }
        filterUpdate(e) {
            self.filter = e.target.value.toLowerCase()
            if (self.filter.trim() === "") {
                self.filter = false
            }
            if (self.throttle) {
                clearTimeout(self.throttle)
            }
            self.throttle = setTimeout(function() {
                riot.store.trigger('filter-changed', self.filter);
            }, 300)
        }
    </script>
</apibook>
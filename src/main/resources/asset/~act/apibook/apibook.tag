<apibook>
    <div class="header">
        <h1>API Book - { sysInfo.appName }</h1>
    </div>
    <div class="content">
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
            width: 75%;
            overflow-x: hidden;
            position: absolute;
        }
        @media print {
            .header {display: none;}
        }
    </style>
    <script>
        var self = this
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
    </script>
</apibook>
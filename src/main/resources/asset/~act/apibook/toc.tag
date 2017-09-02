<toc>
    <div id="toc-container">
        <div class="toc-inner">
            <ul class="toc-list">
                <li class="toc">TOC</li>
                <virtual each="{endpoints}">
                    <li>
                        <a href="#{id}">{description}</a>
                    </li>
                </virtual>
            </ul>
        </div>
    </div>
    <style>
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
        self.endpoints = []
        riot.store.on('endpoints-fetched', function(endpoints) {
            console.log('toc: endpoints-fetched');
            console.log(endpoints);
            self.endpoints = endpoints
            self.update()
        })
    </script>
</toc>
<module-list>
    <div id="module-list-container">
        <div class="module-list-inner">
            <ul class="module-list-list" each={module in filteredModules()}>
                <li class={selected: module.selected} onclick={forceSelect}>{module.name}</li>
            </ul>
        </div>
    </div>
    <style>
        @media print {
            #module-list-container {
                display: none;
            }
        }
        #module-list-container {
            width: 14%;
            position: fixed;
            top: 76px;
            left: 0;
            bottom: 12px;
            overflow-y: auto;
            overflow-x: hidden;
            color: #bbbbbb;
        }
        .module-list-inner {
            overflow-y: visible;
            padding: 4px 0 0 10px;
        }
        .module-list-inner ul.module-list-list {
            list-style-type: none;
            margin: 0;
            padding: 0 8px 0 0;
        }
        .module-list-inner ul.module-list-list li {
            box-sizing: border-box;
            color: #bbb;
            padding: 3px 0 3px 12px;
            position: relative;
            transition: all .3s ease-in-out;
            cursor: pointer;
        }
        .module-list-inner ul.module-list-list li.module-list {
            font-size: 128%;
        }
        .module-list-inner ul.module-list-list:not(.embedded) li:before {
            bottom: 0;
            content: "";
            left: 0;
            position: absolute;
            top: 0;
        }
        .module-list-inner ul.module-list-list li.selected {
            color: #eee;
            background-color: #444;
        }
    </style>
    <script>
        var self = this
        self.modules = [{name: 'All modules', selected: true, all: true, lastSelected: false}]
        self.filter = false
        self.on('mount', function () {
            self.fetchModules()
        })
        filteredModules() {
            if (!self.filter) {
                return self.modules
            } else {
                return self.modules.filter(x => x.name.toLowerCase().includes(self.filter))
            }
        }

        fetchModules() {
            $.getJSON('/~/apibook/modules', function(modules) {
                for (var i = 0, j = modules.length; i < j; ++i) {
                    self.modules.push({
                        name: modules[i],
                        selected: false,
                        all: false,
                        lastSelected: false
                    })
                }
                var selected = []
                for (var i = 1, j = self.modules.length; i < j; ++i) {
                    selected.push(self.modules[i].name)
                }
                riot.store.trigger('module-selected', selected);
                self.update()
            })
        }
        forceSelect(e) {
            if (e.ctrlKey) {
                self.toggleSelect(e)
                return
            }
            var selected = []
            var module = e.item.module;
            for (var i = 0, j = self.modules.length; i < j; ++i) {
                self.modules[i].selected = false;
                self.modules[i].lastSelected = false;
            }
            module.selected = true;
            module.lastSelected = true;
            if (module.all) {
                for (var i = 1, j = self.modules.length; i < j; ++i) {
                    selected.push(self.modules[i].name);
                }
            } else {
                selected.push(module.name)
            }
            riot.store.trigger('module-selected', selected);
        }
        toggleSelect(e) {
            var selected = []
            var module = e.item.module
            module.selected = !module.selected
            if (module.all) {
                if (module.selected) {
                    for (var i = 1, j = self.modules.length; i < j; ++i) {
                        self.modules[i].selected = false
                    }
                } else {
                    var hasSelected = false;
                    for (var i = 1, j = self.modules.length; i < j; ++i) {
                        if (self.modules[i].lastSelected) {
                            self.modules[i].selected = true
                            hasSelected = true
                        }
                    }
                }
            } else {
                module.lastSelected = module.selected;
            }
            for (var i = 1, j = self.modules.length; i < j; ++i) {
                if (self.modules[i].selected) {
                    selected.push(self.modules[i].name)
                }
            }
            if (selected.length == 0) {
                self.modules[0].selected = true
                for (var i = 1, j = self.modules.length; i < j; ++i) {
                    selected.push(self.modules[i].name)
                }
            } else {
                self.modules[0].selected = false
            }
            riot.store.trigger('module-selected', selected);
        }
        riot.store.on('filter-changed', function(filter) {
            self.filter = filter
            self.update()
        })
    </script>
</module-list>
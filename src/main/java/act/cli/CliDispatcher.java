package act.cli;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import act.app.App;
import act.app.AppServiceBase;
import act.cli.builtin.Exit;
import act.cli.builtin.Help;
import act.cli.builtin.IterateCursor;
import act.cli.meta.CommandMethodMetaInfo;
import act.cli.meta.CommanderClassMetaInfo;
import act.handler.CliHandler;
import act.handler.builtin.cli.CliHandlerProxy;
import act.route.Router;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import javax.inject.Inject;
import java.util.*;

/**
 * Dispatch console command to CLI command handler
 */
public class CliDispatcher extends AppServiceBase<CliDispatcher> {

    private static Logger logger = LogManager.get(CliDispatcher.class);
    private static final String NAME_PART_SEPARATOR = "[\\.\\-_]+";

    private Map<Keyword, CliHandler> registry = new HashMap<>();
    private Map<Keyword, String> rawNameRepo = new HashMap<>();
    private Map<String, String> shortCuts = new HashMap<>();
    private Map<String, List<CliHandler>> ambiguousShortCuts = new HashMap<>();
    private Map<CliHandler, List<String>> nameMap = new HashMap<>();
    private Map<CliHandler, List<String>> shortCutMap = new HashMap<>();

    private Router cmdRouter;
    private Router defRouter;

    @Inject
    public CliDispatcher(App app) {
        super(app);
        cmdRouter = app.cliOverHttpRouter();
        if (app.isDev()) {
            defRouter = app.router();
        }
        app.jobManager().now(new Runnable() {
            @Override
            public void run() {
                registerBuiltInHandlers();
            }
        });
        app.jobManager().beforeAppStart(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Keyword, CliHandler> entry : registry.entrySet()) {
                    Keyword keyword = entry.getKey();
                    CliHandler handler = entry.getValue();
                    if (handler instanceof CliHandlerProxy) {
                        CliHandlerProxy proxy = $.cast((handler));
                        CommandMethodMetaInfo methodMetaInfo = proxy.methodMetaInfo();
                        String handlerName = methodMetaInfo.fullName();
                        Set<String> variations = new TreeSet<>();
                        variations.add(keyword.kebabCase());
                        variations.add(keyword.snakeCase());
                        variations.add(keyword.javaVariable());
                        variations.add(keyword.dotted());
                        for (String s : variations) {
                            S.Buffer buf = S.buffer();
                            if (!handlerName.startsWith("act.")) {
                                buf.a("/~/cmd/run/");
                            } else {
                                buf.a("/cmd/run/");
                            }
                            String urlPath = buf.a(s).toString();
                            cmdRouter.addMapping(H.Method.GET, urlPath, handlerName);
                            cmdRouter.addMapping(H.Method.POST, urlPath, handlerName);
                            if (null != defRouter) {
                                defRouter.addMapping(H.Method.GET, urlPath, handlerName);
                                defRouter.addMapping(H.Method.POST, urlPath, handlerName);
                            }
                        }
                    }
                }
            }
        });
    }

    public CliDispatcher registerCommandHandler(String command, CommandMethodMetaInfo methodMetaInfo, CommanderClassMetaInfo classMetaInfo) {
        String sa[] = command.split(CommanderClassMetaInfo.NAME_SEPARATOR);
        for (String s : sa) {
            if (registry.containsKey(s)) {
                throw E.invalidConfiguration("Command %s already registered", command);
            }
            addToRegistry(s, new CliHandlerProxy(classMetaInfo, methodMetaInfo, app()));
            logger.debug("Command registered: %s", s);
        }
        return this;
    }

    public boolean registered(String command) {
        return registry.containsKey(command);
    }

    public CliHandler handler(String command) {
        String command0 = command;
        command = shortCuts.get(command);
        if (null == command) {
            command = command0;
        }
        Keyword keyword = Keyword.of(command);
        CliHandler handler = registry.get(keyword);
        if (null == handler && !command.startsWith("act.")) {
            handler = registry.get(Keyword.of("act." + command));
        }

        Act.Mode mode = Act.mode();
        if (null != handler && handler.appliedIn(mode)) {
            return handler;
        }

        return null;
    }

    /**
     * Returns a list of system commands in alphabetic order
     *
     * @return the system command list
     */
    public List<String> systemCommands() {
        return commands(true, false);
    }

    /**
     * Returns a list of application commands in alphabetic order
     *
     * @return the application command list
     */
    public List<String> applicationCommands() {
        return commands(false, true);
    }

    public SortedSet<CliCmdInfo> commandInfoList(boolean sys, boolean app) {
        SortedSet<CliCmdInfo> list = new TreeSet<>();
        boolean all = !sys && !app;
        for (Map.Entry<Keyword, CliHandler> entry : registry.entrySet()) {
            Keyword keyword = entry.getKey();
            String s = rawNameRepo.get(keyword);
            boolean isSysCmd = s.startsWith("act.");
            if (isSysCmd && !sys && !all) {
                continue;
            }
            if (!isSysCmd && !app && !all) {
                continue;
            }
            CliHandler h = entry.getValue();
            CliCmdInfo info = new CliCmdInfo();
            info.help = h.commandLine()._2;
            info.name = s;
            List<String> shortcuts = shortCuts(h);
            if (null != shortcuts && !shortcuts.isEmpty()) {
                info.shortcut = shortcuts.get(0);
            } else {
                info.shortcut = info.name;
            }
            if (info.shortcut.length() > s.length()) {
                info.shortcut = s;
            }
            if (info.shortcut.startsWith("act.")) {
                info.shortcut = info.shortcut.substring(4);
            }
            list.add(info);
        }
        return list;
    }

    public List<String> commandsWithShortcut(boolean sys, boolean app) {
        return commands0(sys, app, true);
    }

    /**
     * Returns all commands in alphabetic order
     *
     * @return the list of commands
     */
    public List<String> commands(boolean sys, boolean app) {
        return commands0(sys, app, false);
    }

    private List<String> commands0(boolean sys, boolean app, boolean withShortcut) {
        C.List<String> list = C.newList();
        Act.Mode mode = Act.mode();
        boolean all = !sys && !app;
        for (Keyword keyword : registry.keySet()) {
            String s = rawNameRepo.get(keyword);
            boolean isSysCmd = s.startsWith("act.");
            if (isSysCmd && !sys && !all) {
                continue;
            }
            if (!isSysCmd && !app && !all) {
                continue;
            }
            CliHandler h = registry.get(keyword);
            if (h.appliedIn(mode)) {
                if (withShortcut) {
                    List<String> shortcuts = shortCuts(h);
                    if (null != shortcuts && !shortcuts.isEmpty()) {
                        s = s + " | " + shortcuts.get(0);
                    }
                }
                list.add(s);
            }
        }
        return list.sorted(new $.Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                boolean b1 = (o1.startsWith("act."));
                boolean b2 = (o2.startsWith("act."));
                if (b1 & !b2) {
                    return -1;
                }
                if (!b1 & b2) {
                    return 1;
                }
                return o1.compareTo(o2);
            }
        });
    }

    public List<String> names(CliHandler handler) {
        return nameMap.get(handler);
    }

    public List<String> shortCuts(CliHandler handler) {
        return shortCutMap.get(handler);
    }

    @Override
    protected void releaseResources() {
        registry.clear();
    }

    private void addToRegistry0(String name, CliHandler handler) {
        Keyword keyword = Keyword.of(name);
        registry.put(keyword, handler);
        rawNameRepo.put(keyword, name);
    }

    private void addToRegistry(String name, CliHandler handler) {
        addToRegistry0(name, handler);
        Help.updateMaxWidth(name.length());
        updateNameIndex(name, handler);
        registerShortCut(name, handler);
    }

    private void addRouterMapping(String name, CommandMethodMetaInfo methodMetaInfo) {
        if (null != cmdRouter) {
            String handlerName = methodMetaInfo.fullName();
            String urlPath = S.concat("~/cmd/run/", name);
            cmdRouter.addMapping(H.Method.GET, urlPath, methodMetaInfo.fullName());
            cmdRouter.addMapping(H.Method.POST, urlPath, methodMetaInfo.fullName());
        }
    }

    private void resolveCommandPrefix() {
        Map<Keyword, CliHandler> temp = new HashMap<>(registry);
        registry.clear();
        App app = app();
        for (Map.Entry<Keyword, CliHandler> pair : temp.entrySet()) {
            Keyword keyword = pair.getKey();
            String name = rawNameRepo.get(keyword);
            CliHandler handler = pair.getValue();
            if (handler instanceof CliHandlerProxy) {
                CliHandlerProxy proxy = $.cast(handler);
                Class<?> type = app.classForName(proxy.classMetaInfo().className());
                CommandPrefix prefix = type.getAnnotation(CommandPrefix.class);
                if (null != prefix) {
                    String pre = prefix.value();
                    if (S.notBlank(pre)) {
                        name = S.pathConcat(pre, '.', rawNameRepo.get(keyword));
                    }
                }
            }
            addToRegistry(name, handler);
        }
    }

    private void updateNameIndex(String name, CliHandler handler) {
        List<String> nameList = nameMap.get(handler);
        if (null == nameList) {
            nameList = new ArrayList<>();
            nameMap.put(handler, nameList);
        }
        nameList.add(name);
    }

    private void registerShortCut(String name, CliHandler handler) {
        List<String> shortCutNames = shortCutMap.get(handler);
        if (null == shortCutNames) {
            shortCutNames = new ArrayList<>();
            shortCutMap.put(handler, shortCutNames);
        }
        for (int i = 0; i < 5; ++i) {
            String shortCut = shortCut(name, i);
            if (null == shortCut) {
                continue;
            }
            if (ambiguousShortCuts.containsKey(shortCut)) {
                List<CliHandler> list = ambiguousShortCuts.get(shortCut);
                list.add(handler);
            } else if (shortCuts.containsKey(shortCut)) {
                shortCuts.remove(shortCut);
                List<CliHandler> list = new ArrayList<>();
                ambiguousShortCuts.put(shortCut, list);
                for (List<String> ls : shortCutMap.values()) {
                    ls.remove(shortCut);
                }
            } else {
                shortCuts.put(shortCut, name);
                shortCutNames.add(shortCut);
            }
        }
    }

    /**
     * level:
     *
     * 0 - "foo.bar.zee" -> ".fbz"
     * 1 - "foo.bar.zee" -> "f.b.z"
     * 2 - "foo.bar.zee" -> "f.b.zee"
     * 3 - "foo.bar.zee" -> "f.bar.z"
     * 4 - "foo.bar.zee" -> "fo.ba.ze"
     */
    private static String shortCut(String name, int level) {
        String sa[] = name.split(NAME_PART_SEPARATOR);
        if (sa.length < 2) {
            return null;
        }
        S.Buffer sb = S.buffer();
        switch (level) {
            case 0:
                sb.append(".");
                for (String s : sa) {
                    sb.append(s.charAt(0));
                }
                return sb.toString();
            case 1:
                for (String s : sa) {
                    sb.append(s.charAt(0)).append(".");
                }
                sb.deleteCharAt(sb.length() - 1);
                return sb.toString();
            case 2:
                for (int i = 0; i < sa.length - 1; ++i) {
                    sb.append(sa[i].charAt(0)).append(".");
                }
                sb.append(sa[sa.length - 1]);
                return sb.toString();
            case 3:
                for (int i = 0; i < sa.length - 2; ++i) {
                    sb.append(sa[i].charAt(0)).append(".");
                }
                sb.append(sa[sa.length - 2]).append(".");
                sb.append(sa[sa.length - 1].charAt(0));
                return sb.toString();
            case 4:
                for (String s : sa) {
                    sb.append(s.charAt(0));
                    if (s.length() > 1) {
                        sb.append(s.charAt(1));
                    }
                    sb.append(".");
                }
                sb.deleteCharAt(sb.length() - 1);
                return sb.toString();
            default:
                throw E.unsupport();
        }
    }

    private void registerBuiltInHandlers() {
        addToRegistry("act.exit", Exit.INSTANCE);
        addToRegistry("act.quit", Exit.INSTANCE);
        addToRegistry("act.bye", Exit.INSTANCE);
        addToRegistry("act.help", Help.INSTANCE);
        addToRegistry("act.it", IterateCursor.INSTANCE);
    }
}

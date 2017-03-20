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
import org.osgl.Osgl;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.List;
import java.util.Map;

/**
 * Dispatch console command to CLI command handler
 */
public class CliDispatcher extends AppServiceBase<CliDispatcher> {

    private static Logger logger = LogManager.get(CliDispatcher.class);
    private static final String NAME_PART_SEPARATOR = "[\\.\\-_]+";

    private Map<String, CliHandler> registry = C.newMap();
    private Map<String, String> shortCuts = C.newMap();
    private Map<String, List<CliHandler>> ambiguousShortCuts = C.newMap();
    private Map<CliHandler, List<String>> nameMap = C.newMap();
    private Map<CliHandler, List<String>> shortCutMap = C.newMap();

    public CliDispatcher(App app) {
        super(app);
        registerBuiltInHandlers();
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
        CliHandler handler = registry.get(command);
        if (null == handler && !command.startsWith("act.")) {
            handler = registry.get("act." + command);
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

    /**
     * Returns all commands in alphabetic order
     *
     * @return the list of commands
     */
    public List<String> commands(boolean sys, boolean app) {
        C.List<String> list = C.newList();
        Act.Mode mode = Act.mode();
        boolean all = !sys && !app;
        for (String s : registry.keySet()) {
            boolean isSysCmd = s.startsWith("act.");
            if (isSysCmd && !sys && !all) {
                continue;
            }
            if (!isSysCmd && !app && !all) {
                continue;
            }
            CliHandler h = registry.get(s);
            if (h.appliedIn(mode)) {
                list.add(s);
            }
        }
        return list.sorted(new Osgl.Comparator<String>() {
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

    private void addToRegistry(String name, CliHandler handler) {
        registry.put(name, handler);
        Help.updateMaxWidth(name.length());
        updateNameIndex(name, handler);
        registerShortCut(name, handler);
    }

    private void updateNameIndex(String name, CliHandler handler) {
        List<String> nameList = nameMap.get(handler);
        if (null == nameList) {
            nameList = C.newList();
            nameMap.put(handler, nameList);
        }
        nameList.add(name);
    }

    private void registerShortCut(String name, CliHandler handler) {
        List<String> shortCutNames = shortCutMap.get(handler);
        if (null == shortCutNames) {
            shortCutNames = C.newList();
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
                List<CliHandler> list = C.newList();
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

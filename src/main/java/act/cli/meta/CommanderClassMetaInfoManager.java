package act.cli.meta;

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

import static act.Destroyable.Util.destroyAll;

import act.asm.Type;
import act.util.LogSupportedDestroyableBase;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CommanderClassMetaInfoManager extends LogSupportedDestroyableBase {

    private Map<String, CommanderClassMetaInfo> commands = new HashMap<>();
    private Map<Type, List<CommanderClassMetaInfo>> subTypeInfo = new HashMap<>();

    public CommanderClassMetaInfoManager() {
    }

    @Override
    protected void releaseResources() {
        destroyAll(commands.values(), ApplicationScoped.class);
        commands.clear();
        for (List<CommanderClassMetaInfo> l : subTypeInfo.values()) {
            destroyAll(l, ApplicationScoped.class);
        }
        subTypeInfo.clear();
        super.releaseResources();
    }

    public void registerCommanderMetaInfo(CommanderClassMetaInfo metaInfo) {
        String className = Type.getObjectType(metaInfo.className()).getClassName();
        commands.put(className, metaInfo);
    }

    public CommanderClassMetaInfo commanderMetaInfo(String className) {
        return commands.get(className);
    }

}

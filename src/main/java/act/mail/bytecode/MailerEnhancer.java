package act.mail.bytecode;

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

import act.app.App;
import act.asm.ClassVisitor;
import act.asm.MethodVisitor;
import act.asm.Type;
import act.mail.meta.MailerClassMetaInfo;
import act.mail.meta.MailerClassMetaInfoHolder;
import act.mail.meta.SenderMethodMetaInfo;
import act.util.AppByteCodeEnhancer;
import org.osgl.$;

/**
 * Enhance mailer class
 */
public class MailerEnhancer extends AppByteCodeEnhancer<MailerEnhancer> {

    private MailerClassMetaInfoHolder classInfoHolder;
    private String className;

    public MailerEnhancer() {
    }

    public MailerEnhancer(ClassVisitor cv, MailerClassMetaInfoHolder infoHolder) {
        super(_F.isMailer(infoHolder), cv);
        this.classInfoHolder = infoHolder;
    }

    @Override
    public AppByteCodeEnhancer app(App app) {
        this.classInfoHolder = app.classLoader();
        return super.app(app);
    }

    @Override
    protected Class<MailerEnhancer> subClass() {
        return MailerEnhancer.class;
    }

    @Override
    protected void reset() {
        className = null;
        super.reset();
    }

    public MailerEnhancer classInfoHolder(MailerClassMetaInfoHolder holder) {
        classInfoHolder = holder;
        predicate(_F.isMailer(holder));
        return this;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = Type.getObjectType(name).getClassName();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        SenderMethodMetaInfo info = methodInfo(name, access);
        if (null == info) {
            return mv;
        }
        logger.debug(">>>About to enhance mailer method: %s", name);
        return new SenderEnhancer(mv, info, access, name, desc, signature, exceptions);
    }

    private SenderMethodMetaInfo methodInfo(String name, int access) {
        if (isPublic(access) && !isConstructor(name)) {
            MailerClassMetaInfo ccInfo = classInfoHolder.mailerClassMetaInfo(className);
            if (null == ccInfo) {
                return null;
            }
            SenderMethodMetaInfo info = ccInfo.sender(name);
            if (null != info) {
                return info;
            }
            return ccInfo.sender(name);
        } else {
            return null;
        }
    }

    private boolean isTargetMethod(String name, int access) {
        return isPublic(access) && !isConstructor(name) && methodInfo(name, access) != null;
    }

    private enum _F {
        ;

        private static final $.Predicate<String> isMailer(final MailerClassMetaInfoHolder infoSrc) {
            return new $.Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return infoSrc.mailerClassMetaInfo(s) != null;
                }
            };
        }
    }
}

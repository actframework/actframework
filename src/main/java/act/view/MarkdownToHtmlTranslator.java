package act.view;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import act.app.ActionContext;
import act.mail.MailerContext;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.osgl.$;

import java.util.Map;

public class MarkdownToHtmlTranslator extends TemplateBase {

    private TemplateBase upstream;
    private Parser parser;
    HtmlRenderer renderer;


    public MarkdownToHtmlTranslator(TemplateBase upstream) {
        this.upstream = $.requireNotNull(upstream);
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
    }

    @Override
    protected String render(Map<String, Object> renderArgs) {
        return fromMarkdownToHtml(upstream.render(renderArgs));
    }

    @Override
    public String render(ActionContext context) {
        return fromMarkdownToHtml(upstream.render(context));
    }

    @Override
    public String render(MailerContext context) {
        return fromMarkdownToHtml(upstream.render(context));
    }

    @Override
    public boolean supportCache() {
        return true;
    }

    private String fromMarkdownToHtml(String mdContent) {
        Node document = parser.parse(mdContent);
        return renderer.render(document);
    }
}

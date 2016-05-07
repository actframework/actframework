package act.dockit;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.osgl.util.C;

import java.util.List;

/**
 * Implement {@link DocSourceProcessor} using a markdown parser
 *
 * This processor use atlassian's commonmark library to parse markdown
 * source code
 */
public class MarkdownProcessor implements DocSourceProcessor {

    private Parser parser;
    private HtmlRenderer renderer;

    public MarkdownProcessor() {
        parser = Parser.builder().build();
        List<Extension> extensions = C.list(TablesExtension.create(), AutolinkExtension.create(), StrikethroughExtension.create());
        renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    @Override
    public String process(String source) {
        Node document = parser.parse(source);
        return renderer.render(document);
    }
}

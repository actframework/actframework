package act.data;

import org.osgl.util.S;

class ContentTypeWithEncoding {
    public final String contentType;
    public final String encoding;

    public ContentTypeWithEncoding(String contentType, String encoding) {
        this.contentType = contentType;
        this.encoding = encoding;
    }

    public static ContentTypeWithEncoding parse(String contentType) {
        if( contentType == null ) {
            return new ContentTypeWithEncoding("text/html".intern(), null);
        } else {
            String[] contentTypeParts = contentType.split(";");
            String _contentType = contentTypeParts[0].trim().toLowerCase();
            String _encoding = null;
            // check for encoding-info
            if (contentTypeParts.length >= 2) {
                String[] encodingInfoParts = contentTypeParts[1].split(("="));
                if (encodingInfoParts.length == 2 && encodingInfoParts[0].trim().equalsIgnoreCase("charset")) {
                    // encoding-info was found in request
                    _encoding = encodingInfoParts[1].trim();

                    if (S.notBlank(_encoding) &&
                            ((_encoding.startsWith("\"") && _encoding.endsWith("\""))
                                    || (_encoding.startsWith("'") && _encoding.endsWith("'")))
                            ) {
                        _encoding = _encoding.substring(1, _encoding.length() - 1).trim();
                    }
                }
            }
            return new ContentTypeWithEncoding(_contentType, _encoding);
        }
    }
}


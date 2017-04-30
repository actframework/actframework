package act;

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

import act.conf.AppConfig;
import org.osgl.http.H;

import java.util.Locale;

public abstract class ResponseImplBase<T extends ResponseImplBase> extends H.Response<T> {

    protected String charset;
    protected Locale locale;
    protected String contentType;
    private boolean charsetSet;

    protected ResponseImplBase() {}

    protected ResponseImplBase(AppConfig config) {
        charset = config.encoding();
        locale = config.locale();
    }

    @Override
    public String characterEncoding() {
        return charset;
    }

    @Override
    public T characterEncoding(String encoding) {
        charset = encoding;
        charsetSet = true;
        return me();
    }

    @Override
    protected void _setContentType(String type) {
        this.contentType = type;
    }

    protected String _getContentType() {
        return this.contentType != null ? (this.charsetSet ? this.contentType + ";charset=" + this.charset : this.contentType) : null;
    }

    public void commitContentType() {
        header(H.Header.Names.CONTENT_TYPE, _getContentType());
    }

    protected final T me() {
        return (T) this;
    }
}

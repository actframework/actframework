package act.app;

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

import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage {@link AppSourceCodeScanner} and {@link AppByteCodeScanner}
 * for the application
 */
public class AppCodeScannerManager extends AppServiceBase<AppCodeScannerManager> {

    private static final Logger logger = L.get(AppCodeScannerManager.class);

    private List<AppSourceCodeScanner> sourceCodeScanners = new ArrayList<>();
    private List<AppByteCodeScanner> byteCodeScanners = new ArrayList<>();

    public AppCodeScannerManager(App app) {
        super(app);
    }

    public C.List<AppSourceCodeScanner> sourceCodeScanners() {
        return C.list(sourceCodeScanners);
    }

    public C.List<AppByteCodeScanner> byteCodeScanners() {
        return C.list(byteCodeScanners);
    }

    public AppByteCodeScanner byteCodeScannerByClass(Class<? extends AppByteCodeScanner> c) {
        for (AppByteCodeScanner scanner : byteCodeScanners) {
            if (scanner.getClass() == c) {
                return scanner;
            }
        }
        return null;
    }

    public AppCodeScannerManager register(AppSourceCodeScanner sourceCodeScanner) {
        _register(sourceCodeScanner, sourceCodeScanners);
        return this;
    }

    public AppCodeScannerManager register(AppByteCodeScanner byteCodeScanner) {
        _register(byteCodeScanner, byteCodeScanners);
        return this;
    }

    @Override
    protected void releaseResources() {
        sourceCodeScanners.clear();
        byteCodeScanners.clear();
    }

    private <T extends AppCodeScanner> void _register(T scanner, List<T> scanners) {
        scanner.setApp(app());
        if (scanners.contains(scanner)) {
            logger.warn("%s has already been registered", scanner);
            return;
        }
        scanners.add(scanner);
    }

}

/**
 * Copyright (C) 2011 K Venkata Sudhakar <kvenkatasudhakar@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package act.cli.ascii_table;

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


import act.cli.ascii_table.impl.SimpleASCIITableImpl;
import act.cli.ascii_table.spec.IASCIITable;
import act.cli.ascii_table.spec.IASCIITableAware;

/**
 * The entry point to this framework which acts as a singleton.
 * 
 * @author K Venkata Sudhakar (kvenkatasudhakar@gmail.com)
 * @version 1.0
 *
 */
public class ASCIITable implements IASCIITable {

	private static ASCIITable instance = null;
	private IASCIITable asciiTable = new SimpleASCIITableImpl();
	private ASCIITable() {
	}
	
	public static synchronized ASCIITable getInstance() {
		if (instance == null) {
			instance = new ASCIITable();
		}
		return instance;
	}

	@Override
	public String getTable(String[] header, String[][] data) {
		return asciiTable.getTable(header, data);
	}

	@Override
	public String getTable(String[] header, String[][] data, int dataAlign) {
		return asciiTable.getTable(header, data, dataAlign);
	}

	@Override
	public String getTable(String[] header, int headerAlign, String[][] data, int dataAlign) {
		return asciiTable.getTable(header, headerAlign, data, dataAlign);
	}

	public void printTable(String[] header, String[][] data) {
		asciiTable.printTable(header, data);
	}

	@Override
	public void printTable(String[] header, String[][] data, int dataAlign) {
		asciiTable.printTable(header, data, dataAlign);
	}

	@Override
	public void printTable(String[] header, int headerAlign, String[][] data, int dataAlign) {
		asciiTable.printTable(header, headerAlign, data, dataAlign);
	}

	public String getTable(ASCIITableHeader[] headerObjs, String[][] data) {
		return asciiTable.getTable(headerObjs, data);
	}
	
	public void printTable(ASCIITableHeader[] headerObjs, String[][] data) {
		asciiTable.printTable(headerObjs, data);
	}

	@Override
	public String getTable(IASCIITableAware asciiTableAware) {
		return asciiTable.getTable(asciiTableAware);
	}

	@Override
	public void printTable(IASCIITableAware asciiTableAware) {
		asciiTable.printTable(asciiTableAware);
	}
	
}

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
package act.cli.ascii_table.spec;

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


import act.cli.ascii_table.ASCIITableHeader;

/**
 * Interface specifying ASCII table APIs.
 * 
 * @author K Venkata Sudhakar (kvenkatasudhakar@gmail.com)
 * @version 1.0
 *
 */
public interface IASCIITable {
	
	int ALIGN_LEFT = -1;
	int ALIGN_CENTER = 0;
	int ALIGN_RIGHT = 1;
    int ALIGN_AUTO = Integer.MAX_VALUE;

    int DEFAULT_HEADER_ALIGN = ALIGN_CENTER;
	int DEFAULT_DATA_ALIGN = ALIGN_AUTO;
	
	/**
	 * Prints the ASCII table to console.
	 * 
	 * @param header
	 * @param data
	 */
	void printTable(String[] header, String[][] data);
	void printTable(String[] header, String[][] data, int dataAlign);
	void printTable(String[] header, int headerAlign, String[][] data, int dataAlign);
	void printTable(ASCIITableHeader[] headerObjs, String[][] data);
	void printTable(IASCIITableAware asciiTableAware);
	
	/**
	 * Returns the ASCII table as string which can be rendered in console or JSP.
	 * 
	 * @param header
	 * @param data
	 * @return
	 */
	String getTable(String[] header, String[][] data);
	String getTable(String[] header, String[][] data, int dataAlign);
	String getTable(String[] header, int headerAlign, String[][] data, int dataAlign);
	String getTable(ASCIITableHeader[] headerObjs, String[][] data);
	String getTable(IASCIITableAware asciiTableAware);
	
}

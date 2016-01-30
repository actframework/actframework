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

import act.cli.ascii_table.ASCIITableHeader;

/**
 * Interface specifying ASCII table APIs.
 * 
 * @author K Venkata Sudhakar (kvenkatasudhakar@gmail.com)
 * @version 1.0
 *
 */
public interface IASCIITable {
	
	public static final int ALIGN_LEFT = -1;
	public static final int ALIGN_CENTER = 0;
	public static final int ALIGN_RIGHT = 1;
    public static final int ALIGN_AUTO = Integer.MAX_VALUE;

    public static final int DEFAULT_HEADER_ALIGN = ALIGN_CENTER;
	public static final int DEFAULT_DATA_ALIGN = ALIGN_AUTO;
	
	/**
	 * Prints the ASCII table to console.
	 * 
	 * @param header
	 * @param data
	 */
	public void printTable(String[] header, String[][] data);
	public void printTable(String[] header, String[][] data, int dataAlign);
	public void printTable(String[] header, int headerAlign, String[][] data, int dataAlign);
	public void printTable(ASCIITableHeader[] headerObjs, String[][] data);
	public void printTable(IASCIITableAware asciiTableAware);
	
	/**
	 * Returns the ASCII table as string which can be rendered in console or JSP.
	 * 
	 * @param header
	 * @param data
	 * @return
	 */
	public String getTable(String[] header, String[][] data);
	public String getTable(String[] header, String[][] data, int dataAlign);
	public String getTable(String[] header, int headerAlign, String[][] data, int dataAlign);
	public String getTable(ASCIITableHeader[] headerObjs, String[][] data);
	public String getTable(IASCIITableAware asciiTableAware);
	
}

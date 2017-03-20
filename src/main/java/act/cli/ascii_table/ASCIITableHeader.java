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


import act.cli.ascii_table.spec.IASCIITable;

/**
 * Represents ASCII table header.
 * 
 * @author K Venkata Sudhakar (kvenkatasudhakar@gmail.com)
 * @version 1.0
 *
 */
public class ASCIITableHeader {

	private String headerName;
	private int headerAlign = IASCIITable.DEFAULT_HEADER_ALIGN;
	private int dataAlign = IASCIITable.DEFAULT_DATA_ALIGN;

	public ASCIITableHeader(String headerName) {
		this.headerName = headerName;
	}

	public ASCIITableHeader(String headerName, int dataAlign) {
		this.headerName = headerName;
		this.dataAlign = dataAlign;
	}

	public ASCIITableHeader(String headerName, int dataAlign, int headerAlign) {
		this.headerName = headerName;
		this.dataAlign = dataAlign;
		this.headerAlign = headerAlign;
	}

	public String getHeaderName() {
		return headerName;
	}
	
	public void setHeaderName(String headerName) {
		this.headerName = headerName;
	}
	
	public int getHeaderAlign() {
		return headerAlign;
	}
	
	public void setHeaderAlign(int headerAlign) {
		this.headerAlign = headerAlign;
	}
	
	public int getDataAlign() {
		return dataAlign;
	}
	
	public void setDataAlign(int dataAlign) {
		this.dataAlign = dataAlign;
	}
	
}

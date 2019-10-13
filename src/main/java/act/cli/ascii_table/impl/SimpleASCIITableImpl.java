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
package act.cli.ascii_table.impl;

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
import act.cli.ascii_table.spec.IASCIITable;
import act.cli.ascii_table.spec.IASCIITableAware;
import act.util.Banner;
import org.fusesource.jansi.Ansi;
import org.osgl.util.S;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * This implementation builds the header and data  rows
 * by considering each column one by one in the format of 'FFFF...' (border format).
 * While rendering the last column, it closes the table.
 * It takes <max_length_string_in_each_column>+3 characters to render each column. 
 * These three characters include two white spaces at left and right side of 
 * the max length string in the column and one char(|) for table formatting. 
 * 
 * @author K Venkata Sudhakar (kvenkatasudhakar@gmail.com)
 * @version 1.0
 *
 */
public class SimpleASCIITableImpl implements IASCIITable {

	private PrintWriter pw;

	public SimpleASCIITableImpl() {
		pw = new PrintWriter(System.out);
	}

	public SimpleASCIITableImpl(PrintWriter pw) {
		this.pw = pw;
	}

	@Override
	public void printTable(String[] header, String[][] data) {
		printTable(header, DEFAULT_HEADER_ALIGN, data, DEFAULT_DATA_ALIGN);
	}
	
	@Override
	public void printTable(String[] header, String[][] data, int dataAlign) {
		printTable(header, DEFAULT_HEADER_ALIGN, data, dataAlign);
	}

	@Override
	public void printTable(String[] header, int headerAlign, String[][] data, int dataAlign) {
		pw.println(getTable(header, headerAlign, data, dataAlign));
	}
	
	@Override
	public String getTable(String[] header, String[][] data) {
		return getTable(header, DEFAULT_HEADER_ALIGN, data, DEFAULT_DATA_ALIGN);
	}
	
	@Override
	public String getTable(String[] header, String[][] data, int dataAlign) {
		return getTable(header, DEFAULT_HEADER_ALIGN, data, dataAlign);
	}

	@Override
	public String getTable(String[] header, int headerAlign, String[][] data, int dataAlign) {
		ASCIITableHeader[] headerObjs = new ASCIITableHeader[0];
		
		if (header != null && header.length > 0) {
			headerObjs = new ASCIITableHeader[header.length];
			for (int i = 0 ; i < header.length ; i ++) {
				headerObjs[i] = new ASCIITableHeader(header[i], dataAlign, headerAlign);
			}
		}
		return getTable(headerObjs, data);
	}
	
	public void printTable(ASCIITableHeader[] headerObjs, String[][] data) {
		pw.println(getTable(headerObjs, data));
	}
	
	@Override
	public String getTable(IASCIITableAware asciiTableAware) {
		
		ASCIITableHeader[] headerObjs = new ASCIITableHeader[0];
		String[][] data = new String[0][0];

		List<Object> rowData = null;
		ASCIITableHeader colHeader = null;
		
		Vector<String> rowTransData = null;
		String [] rowContent = null;
		String cellData = null;
		
		if (asciiTableAware != null) {
			
			/**
			 * Get the row header.
			 */
			if (asciiTableAware.getHeaders()!= null && 
					!asciiTableAware.getHeaders().isEmpty()) {
				headerObjs = new ASCIITableHeader[asciiTableAware.getHeaders().size()];
				for (int i = 0 ; i < asciiTableAware.getHeaders().size() ; i ++) {
					headerObjs[i] = asciiTableAware.getHeaders().get(i);
				}
			}
			
			/**
			 * Get the data.
			 */
			if (asciiTableAware.getData() != null && !asciiTableAware.getData().isEmpty()) {
				data = new String[asciiTableAware.getData().size()][];
				
				for (int i = 0 ; i < asciiTableAware.getData().size() ; i ++) {
					//Get the row data
					rowData = asciiTableAware.getData().get(i);
					rowTransData = new Vector<String>(rowData.size());
					
					//Transform each cell in the row
					for (int j = 0 ; j < rowData.size() ; j ++) {
						//Get header associated with this cell
						colHeader = j < headerObjs.length ? headerObjs[j] : null;
						
						//Transform the data using the transformation function provided.
						cellData = asciiTableAware.formatData(colHeader, i, j, rowData.get(j));

						ASCIITableHeader header = headerObjs[j];
						int align = header.getDataAlign();
						if (cellData == null) {
							cellData = String.valueOf(rowData.get(j));
							if (IASCIITable.ALIGN_AUTO == align) {
								header.setDataAlign(ALIGN_LEFT);
							}
						} else {
							if (IASCIITable.ALIGN_AUTO == align) {
								header.setDataAlign(ALIGN_RIGHT);
							}
						}
						
						rowTransData.add(cellData);
						
					}//iterate all columns
					
					//Set the transformed content
					rowContent = new String[rowTransData.size()];
					rowTransData.copyInto(rowContent);
					data[i] = rowContent;
					
				}//iterate all rows
				
			}//end data
		}
		
		return getTable(headerObjs, data);
	}

	@Override
	public void printTable(IASCIITableAware asciiTableAware) {
		pw.println(getTable(asciiTableAware));
	}
	
	public String getTable(ASCIITableHeader[] headerObjs, String[][] data) {
	
		if (data == null || data.length == 0) {
			//throw new IllegalArgumentException("Please provide valid data : " + data);
			return "";
		}
		
		/**
		 * Table String buffer
		 */
		StringBuilder tableBuf = S.newBuilder();
		
		/**
		 * Get maximum number of columns across all rows
		 */
		String [] header = getHeaders(headerObjs);
		int colCount = getMaxColumns(header, data);

		/**
		 * Get max length of data in each column
		 */
		List<Integer> colMaxLenList = getMaxColLengths(colCount, header, data);
		
		/**
		 * Check for the existence of header
		 */
		if (header != null && header.length > 0) {
			/**
			 * 1. Row line
			 */
			tableBuf.append(getRowLineBuf(colCount, colMaxLenList, data));
			
			/**
			 * 2. Header line
			 */
			tableBuf.append(getRowDataBuf(colCount, colMaxLenList, header, headerObjs, true, -1));
		}
		
		/**
		 * 3. Data Row lines
		 */
		tableBuf.append(getRowLineBuf(colCount, colMaxLenList, data));
		String [] rowData = null;
		
		//Build row data buffer by iterating through all rows
		for (int i = 0 ; i < data.length ; i++) {
			
			//Build cell data in each row
			rowData = new String [colCount];
			for (int j = 0 ; j < colCount ; j++) {
				
				if (j < data[i].length) {
					rowData[j] = data[i][j];	
				} else {
					rowData[j] = "";
				}
			}
			
			tableBuf.append(getRowDataBuf(colCount, colMaxLenList, rowData, headerObjs, false, i));
		}
		
		/**
		 * 4. Row line
		 */
		tableBuf.append(getRowLineBuf(colCount, colMaxLenList, data));
		return tableBuf.toString();
	}
	
	private String getRowDataBuf(int colCount, List<Integer> colMaxLenList, 
			String[] row, ASCIITableHeader[] headerObjs, boolean isHeader, int rowNo) {
		
		S.Buffer rowBuilder = S.buffer();
		String formattedData;
		int align;
		
		for (int i = 0 ; i < colCount ; i ++) {
		
			align = isHeader ? DEFAULT_HEADER_ALIGN : DEFAULT_DATA_ALIGN;
			
			if (headerObjs != null && i < headerObjs.length) {
				if (isHeader) {
					align = headerObjs[i].getHeaderAlign();
				} else {
					align = headerObjs[i].getDataAlign();
				}
			}
				 
			formattedData = i < row.length ? row[i] : ""; 
			
			//format = "| %" + colFormat.get(i) + "s ";
			formattedData = "| " + 
				getFormattedData(colMaxLenList.get(i), formattedData, align) + " ";
			
			if (i+1 == colCount) {
				formattedData += "|";
			}
			
			rowBuilder.append(formattedData);
		}
		
		String raw = rowBuilder.toString();
		if (rowNo % 2 == 0) {
			raw = Ansi.ansi().render("@|NEGATIVE_ON,FAINT " + raw + "|@").toString();
		} else {
			raw = Ansi.ansi().render("@|BOLD " + raw + "|@").toString();
		}
		return raw + "\n";
	}
	
	private String getFormattedData(int maxLength, String data, int align) {

		int width = widthOf(data);
		
		if (width > maxLength) {
			return data;
		}
		
		boolean toggle = true;
		
		while (widthOf(data) < maxLength) {
			
			if (align == ALIGN_LEFT) {
				data = data + " ";
			} else if (align == ALIGN_RIGHT) {
				data = " " + data;
			} else if (align == ALIGN_CENTER) {
				if (toggle) {
					data = " " + data;
					toggle = false;
				} else {
					data = data + " ";
					toggle = true;
				}
			}
		}
		
		return data;
	}
	
	/**
	 * Each string item rendering requires the border and a space on both sides.
	 * 
	 * 12   3   12      3  12    34 
	 * +-----   +--------  +------+
	 *   abc      venkat     last
	 * 
	 * @param colCount
	 * @param colMaxLenList
	 * @param data
	 * @return
	 */
	private String getRowLineBuf(int colCount, List<Integer> colMaxLenList, String[][] data) {
		
		S.Buffer rowBuilder = S.buffer();
		int colWidth;
		
		for (int i = 0 ; i < colCount ; i ++) {
			
			colWidth = colMaxLenList.get(i) + 3;
			
			for (int j = 0; j < colWidth ; j ++) {
				if (j==0) {
					rowBuilder.append("+");
				} else if ((i+1 == colCount && j+1 == colWidth)) {//for last column close the border
					rowBuilder.append("-+");
				} else {
					rowBuilder.append("-");
				}
			}
		}
		
		return rowBuilder.append("\n").toString();
	}
	
	private int getMaxItemLength(List<String> colData) {
		int maxLength = 0;
		for (int i = 0 ; i < colData.size() ; i ++) {
			maxLength = Math.max(widthOf(colData.get(i)), maxLength);
		}
		return maxLength;
	}

	private int getMaxColumns(String [] header, String[][] data) {
		int maxColumns = 0;
		for (int i = 0; i < data.length; i++) {
			maxColumns = Math.max(data[i].length, maxColumns);
		}
		maxColumns = Math.max(header.length, maxColumns);
		return maxColumns;
	}
	
	private List<Integer> getMaxColLengths(int colCount, String[] header, String[][] data) {

		List<Integer> colMaxLenList = new ArrayList<Integer>(colCount);
		List<String> colData;
		int maxLength;
		
		for (int i = 0 ; i < colCount ; i ++) {
			colData = new ArrayList<>();
			
			if (header != null && i < header.length) {
				colData.add(header[i]);
			}
			
			for (int j = 0 ; j < data.length; j ++) {
				if (i < data[j].length) {
					colData.add(data[j][i]);	
				} else {
					colData.add("");
				}
			}
			
			maxLength = getMaxItemLength(colData);
			colMaxLenList.add(maxLength);
		}
		
		return colMaxLenList;
	}
	
	private String [] getHeaders(ASCIITableHeader[] headerObjs) {
		String [] header = new String[0];
		if (headerObjs != null && headerObjs.length > 0) {
			header = new String[headerObjs.length];
			for (int i = 0 ; i < headerObjs.length ; i ++) {
				header[i] = headerObjs[i].getHeaderName();
			}
		}
		
		return header;
	}

	private int widthOf(String s) {
		// mult-bytes char occupies 2 column in CLI, so we need to count that in
		int width = 0, len = s.length();
		for (int i = 0; i < len; ++i) {
			char c = s.charAt(i);
			width += (c > 255) ? 2 : 1;
		}
		return width;
	}

}

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


import act.cli.CliContext;
import act.cli.ascii_table.ASCIITableHeader;
import act.cli.ascii_table.spec.IASCIITableAware;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/**
 * This class is useful to extract the header and row data from
 * a list of java beans.
 *  
 * @author K Venkata Sudhakar (kvenkatasudhakar@gmail.com)
 * @author Gelin Luo (greenlaw110@gmail.com) - adapt to actframework
 * @version act-1.0
 *
 */
public class CollectionASCIITableAware<T> implements IASCIITableAware {

	private List<ASCIITableHeader> headers = null;
	private List<List<Object>> data = null;
	
	public CollectionASCIITableAware(List<T> objList, String ... properties) {
		this(objList, Arrays.asList(properties), Arrays.asList(properties));
	}
	
	public CollectionASCIITableAware(List<T> objList, List<String> properties, List<String> title) {
		if (objList != null && !objList.isEmpty() && properties != null && !properties.isEmpty()) {
			//Populate header
			String header;
			headers = new ArrayList<>(properties.size());
			int titleSize = title.size();
			properties = new ArrayList<>(properties);
			for (int i = 0 ; i < properties.size() ; i ++) {
				String prop = properties.get(i);
				header = prop;
				if (i < titleSize) {
					header = title.get(i);
					if (null == header) {
						header = prop;
					}
				}
				int pos = header.indexOf(" as ");
				if (pos > -1) {
					header = header.substring(pos + 4).trim();
					prop = prop.substring(0, pos).trim();
					properties.remove(i);
					properties.add(i, prop);
				}
				headers.add(new ASCIITableHeader(Keyword.of(header).constantName()));
			}
			
			//Populate data
			data = new ArrayList<>();
			List<Object> rowData;
			Class<?> dataClazz = Object.class;
			for (Object o: objList) {
				if (null != o) {
					dataClazz = o.getClass();
					break;
				}
			}
			CacheService cache = null;
			CliContext ctx = CliContext.current();
			if (null != ctx) {
				cache = ctx.evaluatorCache();
			}
			for (int i = 0 ; i < objList.size() ; i ++) {
				rowData = new ArrayList<>();
				
				for (int j = 0 ; j < properties.size() ; j ++) {
					rowData.add(getProperty(cache,
							dataClazz, objList.get(i), properties.get(j)));
				}
				
				data.add(rowData);
			}//iterate rows
			
		}
	}

	private Object getProperty(CacheService evaluatorCache, Class<?> dataClazz, T obj, String property) {
		if (S.eq("this", property)) {
			return obj;
		}
		return $.getProperty(evaluatorCache, obj, property);
	}
	
	private Method getMethod(Class<?> dataClazz, String methodName) {
		Method method = null;
		try {
			method = dataClazz.getMethod(methodName, new Class<?>[] {});
		} catch (Exception e) {
		}
		return method;
	}
	
	private String capitalize(String property) {
		return property.length() == 0 ? property : 
			property.substring(0, 1).toUpperCase() + property.substring(1).toLowerCase();
	}
	
	@Override
	public List<List<Object>> getData() {
		return data;
	}

	@Override
	public List<ASCIITableHeader> getHeaders() {
		return headers;
	}

	@Override
	public String formatData(ASCIITableHeader header, int row, int col, Object data) {
        if (null == data) {
            return "";
        }
		//Format only numbers
		try {
			BigDecimal bd = new BigDecimal(S.string(data));
			return DecimalFormat.getInstance().format(bd);
		} catch (Exception e) {
		}

		//For non-numbers return null 
		return null;
	}
}

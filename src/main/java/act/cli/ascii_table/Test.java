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


import act.cli.ascii_table.impl.CollectionASCIITableAware;
import act.cli.ascii_table.spec.IASCIITableAware;

import java.util.Arrays;
import java.util.List;

/**
 * ASCII Table test cases.
 * 
 * @author K Venkata Sudhakar (kvenkatasudhakar@gmail.com)
 * @version 1.0
 *
 */
public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		basicTests();
		//h2JDBCTests();
		//oracleJDBCTests();
		collectionTests();
	}
	
	private static void basicTests() {
		
		String [] header = { "User Name", 
	    		"Salary", "Designation",
	    		"Address", "Lucky#"
	    		};
		
	    String[][] data = {
	    		{ "Ram", "2000", "Manager", "#99, Silk board", "1111"  },
	    		{ "Sri", "12000", "Developer", "BTM Layout", "22222" },
	    		{ "Prasad", "42000", "Lead", "#66, Viaya Bank Layout", "333333" },
	    		{ "Anu", "132000", "QA", "#22, Vizag", "4444444" },
	    		{ "Sai", "62000", "Developer", "#3-3, Kakinada"  },
	    		{ "Venkat", "2000", "Manager"   },
	    		{ "Raj", "62000"},
	    		{ "BTC"},
	    };
	    
	    //ASCIITable.getInstance().printTable(header, ASCIITable.ALIGN_RIGHT, data, ASCIITable.ALIGN_LEFT);
	    //ASCIITable.getInstance().printTable(header, data, ASCIITable.ALIGN_LEFT);
	    
	    ASCIITableHeader[] headerObjs = {
	    		new ASCIITableHeader("User Name", ASCIITable.ALIGN_LEFT),
	    		new ASCIITableHeader("Salary"),
	    		new ASCIITableHeader("Designation", ASCIITable.ALIGN_CENTER),
	    		new ASCIITableHeader("Address", ASCIITable.ALIGN_LEFT),
	    		new ASCIITableHeader("Lucky#", ASCIITable.ALIGN_RIGHT),
	    };
	    
	    ASCIITable.getInstance().printTable(headerObjs, data);
	    ASCIITable.getInstance().printTable(header, data);
	}

	private static void collectionTests() {
	
		Employee stud = new Employee("Sriram", 2, "Chess", false, 654321.21d, "15 King street Woolooware", "0411010120");
		Employee stud2 = new Employee("Sudhakar", 29, "Painting", true, 123456789.12d, "5/16 real street barry", "0453010113");
	    List<Employee> students = Arrays.asList(stud, stud2);
	 
	    IASCIITableAware asciiTableAware =
	    	new CollectionASCIITableAware<Employee>(students,
	    			"name", "age", "married", "salary");  //properties to read
	    ASCIITable.getInstance().printTable(asciiTableAware);
	    
	    
	    asciiTableAware = 
	    	new CollectionASCIITableAware<Employee>(students, 
	    			Arrays.asList("name", "age", "married", "hobby", "salary", "contact.mobile as mobile", "contact/address as address"), //properties to read
	    			Arrays.asList("STUDENT_NAME", "HIS_AGE")); //custom headers
	    ASCIITable.getInstance().printTable(asciiTableAware);
	}

	public static class Contact {
		private String address;
		private String mobile;
		private Contact() {}

		public Contact(String address, String mobile) {
			this.address = address;
			this.mobile = mobile;
		}
	}

	public static class Employee {
	
		private String name;
		private int age;
		private String hobby;
		private boolean married;
		private double salary;
		private Contact contact;
	
		public Employee(String name, int age, String hobby, boolean married, double salary, String address, String mobile) {
			super();
			this.name = name;
			this.age = age;
			this.hobby = hobby;
			this.married = married;
			this.salary = salary;
			this.contact = new Contact(address, mobile);
		}
	
		public String getName() {
			return name;
		}
	
		public void setName(String name) {
			this.name = name;
		}
	
		public int getAge() {
			return age;
		}
	
		public void setAge(int age) {
			this.age = age;
		}
	
		public String getHobby() {
			return hobby;
		}
	
		public void setHobby(String hobby) {
			this.hobby = hobby;
		}
	
		public boolean isMarried() {
			return married;
		}

		public void setMarried(boolean married) {
			this.married = married;
		}

		public double getSalary() {
			return salary;
		}

		public void setSalary(double salary) {
			this.salary = salary;
		}
	}
	
}

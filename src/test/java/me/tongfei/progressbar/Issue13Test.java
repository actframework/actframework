package me.tongfei.progressbar;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import org.junit.Test;

/**
 * @author bwittwer
 */
public class Issue13Test {

	private static final int NBR_ELEMENTS = 100;
	private static final int PROGRESSBAR_GRACE_PERIOD = 1000;

	@Test
	public void testOk() {
		ProgressBar pb = new ProgressBar("Test", NBR_ELEMENTS);
		pb.start();

		try {
			Thread.sleep(PROGRESSBAR_GRACE_PERIOD);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		for (int i = 0; i < 100; i++) {
			pb.step();
		}

		pb.stop();
	}

	@Test
	public void testKo() {
		ProgressBar pb = new ProgressBar("Test", NBR_ELEMENTS);
		pb.start();

		for (int i = 0; i < 100; i++) {
			pb.step();
		}

		pb.stop();
	}

}

/*
 * Copyright 2012-2015 the original author or authors.
 *
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
 */

package org.springframework.boot.reload;

import java.util.Map;
import java.util.Properties;

public class Remote {

	public static void main(String[] args) {
		System.out.println("Hello There");
		try {
			someCrap();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("restriction")
	private static void someCrap() throws Exception {

		dump(sun.misc.VMSupport.getAgentProperties());

		// ApplicationPid pid = new ApplicationPid();
		// // FIXME find jar
		// Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
		// Method attachMethod = vmClass.getDeclaredMethod("attach", String.class);
		// Object vm = attachMethod.invoke(null, pid.toString());
		// Method getAgentProperties = vmClass.getDeclaredMethod("getAgentProperties");
		// Properties properties = (Properties) getAgentProperties.invoke(vm);
		// dump(properties);
	}

	private static void dump(Properties properties) {
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			System.out.println(entry);
		}
	}

}

/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.atlantisframework.vortex.metric;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * Result
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
@Getter
@Setter
public class Result {

	private final String dataType;
	private final String name;
	private final String metric;
	private Map<String, Map<String, Object>> data;

	public Result(String dataType, String name, String metric) {
		this.dataType = dataType;
		this.name = name;
		this.metric = metric;
	}

}

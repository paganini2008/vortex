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
package io.atlantisframework.vortex;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.io.FileUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * Summary
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
@Getter
@Setter
public final class Summary {

	private long tps;
	private long count;
	private long length;
	private long timestamp;
	private final Map<String, Summary> children = new ConcurrentHashMap<String, Summary>();

	Summary() {
	}

	public String getFormattedLength() {
		return FileUtils.formatSize(length);
	}

	public Map<String, Object> toEntries() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("tps", getTps());
		data.put("count", getCount());
		data.put("length", getLength());
		data.put("timestamp", getTimestamp());
		return data;
	}

}

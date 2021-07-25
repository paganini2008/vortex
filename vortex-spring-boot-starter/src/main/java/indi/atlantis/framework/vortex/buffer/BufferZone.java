/**
* Copyright 2017-2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.vortex.buffer;

import java.util.List;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BufferZone
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
public interface BufferZone {

	static String DEFAULT_COLLECTION_NAME_PREFIX = "atlantis:framework:vortex:bufferzone";

	default void setCollectionNamePrefix(String namePrefix, String subNamePrefix) {
	}

	void set(String collectionName, Tuple tuple) throws Exception;

	List<Tuple> get(String collectionName, int pullSize) throws Exception;

	long size(String collectionName) throws Exception;

}

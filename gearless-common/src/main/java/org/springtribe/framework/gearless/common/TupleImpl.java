package org.springtribe.framework.gearless.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.paganini2008.devtools.beans.BeanUtils;
import com.github.paganini2008.devtools.converter.ConvertUtils;

/**
 * 
 * TupleImpl
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public class TupleImpl extends HashMap<String, Object> implements Tuple, Serializable {

	private static final long serialVersionUID = 959427866779280701L;

	public TupleImpl() {
		super();
		setField("timestamp", System.currentTimeMillis());
	}

	@Override
	public boolean hasField(String fieldName) {
		return containsKey(fieldName);
	}

	@Override
	public void setField(String fieldName, Object value) {
		put(fieldName, value);
	}

	@Override
	public Object getField(String fieldName) {
		return get(fieldName);
	}

	@Override
	public <T> T getField(String fieldName, Class<T> requiredType) {
		return ConvertUtils.convertValue(getField(fieldName), requiredType);
	}

	@Override
	public void fill(Object object) {
		for (String key : keySet()) {
			BeanUtils.setProperty(object, key, get(key));
		}
	}

	@Override
	public void append(Map<String, ?> m) {
		putAll(m);
	}

	@Override
	public Map<String, Object> toMap() {
		return Collections.unmodifiableMap(this);
	}

	@Override
	public Tuple copy() {
		return Tuple.wrap(this);
	}

}

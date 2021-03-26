package indi.atlantis.framework.vortex;

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
 * @author Jimmy Hoff
 *
 * @version 1.0
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

package indi.atlantis.framework.vortex.common;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * 
 * Env
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class Env {

	public static int getPid() {
		try {
			RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
			return Integer.parseInt(runtimeMXBean.getName().split("@")[0]);
		} catch (Exception e) {
			return 0;
		}
	}

}

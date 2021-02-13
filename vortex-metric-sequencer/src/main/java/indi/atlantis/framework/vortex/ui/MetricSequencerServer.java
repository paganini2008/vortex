package indi.atlantis.framework.vortex.ui;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.github.paganini2008.devtools.io.FileUtils;
import com.github.paganini2008.devtools.net.NetUtils;

import indi.atlantis.framework.seafloor.Constants;
import indi.atlantis.framework.vortex.common.Env;
import indi.atlantis.framework.vortex.sequence.EnableMetricSequencerServer;

/**
 * 
 * MetricSequencerServer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@EnableMetricSequencerServer
@SpringBootApplication
public class MetricSequencerServer {

	static {
		System.setProperty("spring.devtools.restart.enabled", "false");
		File logDir = FileUtils.getFile(FileUtils.getUserDirectory(), "logs", "indi", "atlantis", "framework", "vortex");
		if (!logDir.exists()) {
			logDir.mkdirs();
		}
		System.setProperty("LOG_BASE", logDir.getAbsolutePath());
	}

	public static void main(String[] args) {
		int port = NetUtils.getRandomPort(Constants.RANDOM_PORT_RANGE_START, Constants.RANDOM_PORT_RANGE_END);
		port = 12000;
		System.setProperty("server.port", String.valueOf(port));
		SpringApplication.run(MetricSequencerServer.class, args);
		System.out.println(Env.getPid());
	}

}

package indi.atlantis.framework.vortex.metrics;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.github.paganini2008.devtools.Env;
import com.github.paganini2008.devtools.io.FileUtils;

import indi.atlantis.framework.vortex.sequence.EnableMetricSequencerServer;

/**
 * 
 * MetricsApplicationMain
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@EnableMetricSequencerServer
@SpringBootApplication
public class MetricsApplicationMain {

	static {
		System.setProperty("spring.devtools.restart.enabled", "false");
		File logDir = FileUtils.getFile(FileUtils.getUserDirectory(), "logs", "indi", "atlantis", "framework", "vortex");
		if (!logDir.exists()) {
			logDir.mkdirs();
		}
		System.setProperty("LOG_BASE", logDir.getAbsolutePath());
	}

	public static void main(String[] args) {
		SpringApplication.run(MetricsApplicationMain.class, args);
		System.out.println(Env.getPid());
	}

}

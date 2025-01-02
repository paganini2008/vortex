package com.github.vortex.web;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.github.doodler.common.Constants;
import com.github.doodler.common.utils.NetUtils;
import com.github.vortex.tsd.EnableTsdStoreServer;

/**
 * 
 * @Description: TssApplicationMain
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@EnableTsdStoreServer
@SpringBootApplication
public class TssApplicationMain {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    public static void main(String[] args) {
        int serverPort =
                NetUtils.getRandomPort(Constants.SERVER_PORT_FROM, Constants.SERVER_PORT_TO);
        System.setProperty("server.port", String.valueOf(serverPort));
        System.out.println("serverPort: " + serverPort);
        SpringApplication.run(TssApplicationMain.class, args);
    }
}

package indi.atlantis.framework.vortex.metrics;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 
 * MetricPage
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
@RequestMapping("/metric")
@Controller
public class MetricPage {

	@GetMapping("/")
	public String index(Model ui) {
		return "index";
	}

}

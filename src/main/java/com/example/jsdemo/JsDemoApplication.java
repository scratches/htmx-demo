package com.example.jsdemo;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

@SpringBootApplication
@Controller
public class JsDemoApplication {

	private final ObjectMapper mapper;

	public JsDemoApplication(ObjectMapper objectMapper) {
		this.mapper = objectMapper;
	}

	@GetMapping("/user")
	@ResponseBody
	public String user() {
		return "Fred";
	}

	@PostMapping("/greet")
	@ResponseBody
	public String greet(@ModelAttribute Greeting values) {
		return "Hello " + values.getValue() + "!";
	}

	@GetMapping("/time")
	@ResponseBody
	public String time() {
		return "Time: " + System.currentTimeMillis();
	}

	@GetMapping("/notify")
	@ResponseBody
	public ResponseEntity<Void> notification() throws Exception {
		return ResponseEntity.status(HttpStatus.CREATED)
				.header("HX-Trigger", mapper.writeValueAsString(Map.of("notice", "Notification"))).build();
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(SseViewBuilder builder) {
		return builder.stream(Flux.interval(Duration.ofSeconds(5)).map(
				value -> new ModelAndView("time", Map.of("value", value, "time", System.currentTimeMillis())))).build();
	}

	@GetMapping(path = "/test")
	public List<ModelAndView> test() {
		return List.of(new ModelAndView("test", Map.of("id", "hello", "value", "Hello")),
				new ModelAndView("test", Map.of("id", "world", "value", "World")));
	}

	public static void main(String[] args) {
		SpringApplication.run(JsDemoApplication.class, args);
	}

	static class Greeting {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

}
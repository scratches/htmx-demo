package com.example.jsdemo;

import java.util.Map;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

public class EventBuilder {

	private String viewName;
	private Map<String, Object> model;

	public EventBuilder(String view, Map<String, Object> model) {
		this.viewName = view;
		this.model = model;
	}

	public static EventBuilder of(String view, Map<String, Object> model) {
		return new EventBuilder(view, model);
	}

	public ServerSentEvent<String> build() {
		String data = "";
		ServletWebRequest webRequest = (ServletWebRequest) RequestContextHolder.getRequestAttributes()
				.getAttribute("wrapper.request", RequestAttributes.SCOPE_REQUEST);
		ViewResolver resolver = (ViewResolver) RequestContextHolder.getRequestAttributes().getAttribute("view.resolver",
				RequestAttributes.SCOPE_REQUEST);
		try {
			View view = resolver.resolveViewName(viewName, webRequest.getRequest().getLocale());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to resolve view", e);
		}
		return ServerSentEvent.builder(data).build();
	}

}

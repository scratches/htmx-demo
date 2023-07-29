package com.example.jsdemo;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

@Component
public class SseViewHelper {

	private ViewResolver resolver;

	private final ApplicationContext context;

	public SseViewHelper(ApplicationContext context) {
		this.context = context;
	}

	private void initialize() {
		if (this.resolver == null) {
			this.resolver = context.getBean("viewResolver", ViewResolver.class);
		}
		if (this.resolver instanceof ContentNegotiatingViewResolver content) {
			// We don't need the content negotation, so we just use MediaType.ALL
			ContentNegotiatingViewResolver updated = new ContentNegotiatingViewResolver() {
				protected List<MediaType> getMediaTypes(HttpServletRequest request) {
					return List.of(MediaType.ALL);
				}
				public void afterPropertiesSet() {
					super.afterPropertiesSet();
					this.initApplicationContext(context);
					this.initServletContext(getServletContext());
				}
			};
			updated.setContentNegotiationManager(content.getContentNegotiationManager());
			updated.setApplicationContext(context);
			updated.afterPropertiesSet();
			this.resolver = updated;
		}
	}

	public SseEmitter stream(HttpServletRequest request, HttpServletResponse response, String viewName,
			Flux<Map<String, Object>> models) {

		initialize();
		SseEmitter emitter = new SseEmitter();

		try {

			View view = resolver.resolveViewName(viewName, request.getLocale());
			if (view == null) {
				emitter.completeWithError(new IllegalStateException("View not found: " + viewName));
			} else {
				models.map(
						value -> new ModelAndView(view, value))
						.subscribe(
								modelAndView -> {
									try {

										ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(
												response);
										modelAndView.getView().render(modelAndView.getModel(), request, wrapper);
										wrapper.flushBuffer();
										emitter.send(wrapper.getContentAsByteArray(), MediaType.TEXT_HTML);

									} catch (Exception e) {
										emitter.completeWithError(e);
									}
								}, emitter::completeWithError, emitter::complete);
			}
		} catch (Exception e) {
			emitter.completeWithError(e);
		}

		return emitter;

	}
}

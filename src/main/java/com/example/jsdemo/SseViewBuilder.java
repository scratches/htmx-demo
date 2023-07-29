package com.example.jsdemo;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

public class SseViewBuilder {

	private final ViewResolver resolver;
	private final HttpServletRequest request;
	private final HttpServletResponse response;

	private Flux<ModelAndView> models = Flux.empty();

	public SseViewBuilder(ViewResolver resolver, HttpServletRequest request, HttpServletResponse response) {
		this.resolver = resolver;
		this.request = request;
		this.response = response;
	}

	public SseEmitter build() {

		SseEmitter emitter = new SseEmitter();

		try {

			models.map(
					value -> {
						if (!(value.getView() instanceof View)) {
							try {
								RequestContextHolder
										.setRequestAttributes(new ServletRequestAttributes(request, response));
								View view = resolver.resolveViewName(value.getViewName(), request.getLocale());
								RequestContextHolder.resetRequestAttributes();
								if (view == null) {
									emitter.completeWithError(
											new IllegalStateException("View not found: " + value.getViewName()));
								} else {
									value.setView(view);
								}
							} catch (Exception e) {
								emitter.completeWithError(e);
							}
						}
						return value;
					})
					.subscribe(
							modelAndView -> {
								try {
									String text = render(modelAndView);
									// https://github.com/spring-projects/spring-framework/issues/30965
									emitter.send(text.replace("\n", "\ndata:"), MediaType.TEXT_HTML);
								} catch (Exception e) {
									emitter.completeWithError(e);
								}
							}, emitter::completeWithError, emitter::complete);
		} catch (Exception e) {
			emitter.completeWithError(e);
		}

		return emitter;

	}

	private String render(ModelAndView modelAndView) throws Exception {
		ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(
				response);
		modelAndView.getView().render(modelAndView.getModel(), request, wrapper);
		wrapper.flushBuffer();
		return new String(wrapper.getContentAsByteArray());
	}

	public SseViewBuilder stream(Flux<ModelAndView> map) {
		this.models = map;
		return this;
	}
}

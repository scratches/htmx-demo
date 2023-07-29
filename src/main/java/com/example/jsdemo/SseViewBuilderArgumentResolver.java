package com.example.jsdemo;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SseViewBuilderArgumentResolver
		implements HandlerMethodArgumentResolver, WebMvcConfigurer {

	private ViewResolver resolver;

	private final ApplicationContext context;

	public SseViewBuilderArgumentResolver(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(this);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType().equals(SseViewBuilder.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		initialize();
		return new SseViewBuilder(resolver, webRequest.getNativeRequest(HttpServletRequest.class),
				webRequest.getNativeResponse(HttpServletResponse.class));
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

}

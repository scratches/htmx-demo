package com.example.jsdemo;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CompositeViewRenderer implements HandlerInterceptor, WebMvcConfigurer {

	private static Log logger = LogFactory.getLog(CompositeViewRenderer.class);

	private ViewResolver resolver;

	private final ApplicationContext context;

	public CompositeViewRenderer(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(new ServerSentEventHttpMessageConverter());
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this);
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

		initialize();
		if (modelAndView == null || !HandlerMethod.class.isInstance(handler)) {
			return;
		}

		HandlerMethod method = (HandlerMethod) handler;
		if (!supportsReturnType(method.getReturnType())) {
			return;
		}

		String attribute = "modelAndViewList";
		handleReturnValue(modelAndView.getModel().get(attribute), method.getReturnType(), modelAndView, response);

	}

	private boolean supportsReturnType(MethodParameter returnType) {
		if (List.class.isAssignableFrom(returnType.getParameterType())) {
			if (ModelAndView.class
					.isAssignableFrom(ResolvableType.forMethodParameter(returnType).getGeneric().resolve())) {
				return true;
			}
		}
		return false;
	}

	private void initialize() {
		if (this.resolver == null) {
			this.resolver = context.getBean("viewResolver", ViewResolver.class);
		}
	}

	private void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndView mavContainer, HttpServletResponse response) throws Exception {
		String[] methodAnnotation = returnType
				.getMethodAnnotation(RequestMapping.class).produces();
		MediaType type = methodAnnotation.length > 0 ? MediaType.valueOf(methodAnnotation[0]) : MediaType.TEXT_HTML;
		response.setContentType(type.toString());
		@SuppressWarnings("unchecked")
		List<ModelAndView> renderings = resolve(response, (List<ModelAndView>) returnValue);
		mavContainer.setView(new CompositeView(renderings));
	}

	private List<ModelAndView> resolve(HttpServletResponse response, List<ModelAndView> renderings) {
		for (ModelAndView rendering : renderings) {
			try {
				resolve(response, rendering);
			} catch (Exception e) {
				logger.error("Failed to resolve view", e);
			}
		}
		return renderings;
	}

	private void resolve(HttpServletResponse response, ModelAndView rendering) throws Exception {
		if (!(rendering.getView() instanceof View)) {
			Locale locale = response.getLocale();
			if (locale == null) {
				locale = Locale.getDefault();
			}
			View view = resolver.resolveViewName((String) rendering.getViewName(), locale);
			rendering.setView(view);
		}
	}

	static class CompositeView implements View {

		private List<ModelAndView> renderings;

		public CompositeView(List<ModelAndView> renderings) {
			this.renderings = renderings;
		}

		@Override
		public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			for (ModelAndView rendering : renderings) {
				rendering.getView().render(rendering.getModel(), request, response);
				response.getWriter().write("\n\n");
			}
		}

	}

	class ServerSentEventHttpMessageConverter extends AbstractHttpMessageConverter<ModelAndView> {

		public ServerSentEventHttpMessageConverter() {
			super(MediaType.APPLICATION_JSON);
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return ModelAndView.class.isAssignableFrom(clazz);
		}

		@Override
		protected ModelAndView readInternal(Class<? extends ModelAndView> clazz, HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			throw new UnsupportedOperationException("Unimplemented method 'readInternal'");
		}

		@Override
		protected void writeInternal(ModelAndView wrapper, HttpOutputMessage outputMessage)
				throws IOException, HttpMessageNotWritableException {
			ServletWebRequest webRequest = (ServletWebRequest) RequestContextHolder.getRequestAttributes()
					.getAttribute("wrapper.request", RequestAttributes.SCOPE_REQUEST);
			try {
				resolve(webRequest.getResponse(), wrapper);
				wrapper.getView().render(wrapper.getModel(), webRequest.getRequest(), webRequest.getResponse());
			} catch (Exception e) {
				throw new IllegalStateException("Failed to render view", e);
			}
		}
	}

}
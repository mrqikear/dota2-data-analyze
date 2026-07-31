package com.dota2.api.config;

import com.dota2.api.filter.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import java.net.Socket;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${app.proxy.host:}")
    private String proxyHost;

    @Value("${app.proxy.port:0}")
    private int proxyPort;

    /**
     * Check if the proxy is actually reachable.
     */
    private boolean proxyReachable() {
        if (proxyHost.isEmpty() || proxyPort <= 0) return false;
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(proxyHost, proxyPort), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 不拦截静态资源和前端页面路由（包括 SPA 路由名）。
     * API 端点仍然受保护（前端通过 ssoToken header 认证）。
     */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            // 登录等公开 API
            "/user/login",
            "/user/getRSAPublicKey",
            // Swagger
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/v2/api-docs",
            // 错误
            "/error",
            // 前端静态资源
            "/",
            "/index.html",
            "/favicon.svg",
            "/icons.svg",
            "/assets/**",
            // 资产缓存（图标等）
            "/asset/**",
            // 游戏常量
            "/constants/**",
            // 前端 SPA 路由
            "/login",
            "/dashboard",
            "/user",
            "/steamAccount",
            "/steamAccount/**",
            "/match",
            "/match/**",
            "/analysis",
            "/analysis/**"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String distPath = "file:D:/dota2-data-analyze/dota2-frontend/dist/";
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", distPath)
                .setCachePeriod(3600);
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        boolean useProxy = proxyReachable();
        if (useProxy) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
            log.info("RestTemplate using proxy {}:{}", proxyHost, proxyPort);
        } else if (!proxyHost.isEmpty() && proxyPort > 0) {
            log.warn("Proxy {}:{} configured but unreachable, falling back to direct connection", proxyHost, proxyPort);
        }

        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(30_000);

        return new RestTemplate(factory);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(c -> c instanceof MappingJackson2HttpMessageConverter)
                .map(c -> (MappingJackson2HttpMessageConverter) c)
                .forEach(converter -> {
                    ObjectMapper objectMapper = converter.getObjectMapper();
                    SimpleModule simpleModule = new SimpleModule();
                    simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
                    simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
                    objectMapper.registerModule(simpleModule);
                    objectMapper.registerModule(new JavaTimeModule());
                });
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

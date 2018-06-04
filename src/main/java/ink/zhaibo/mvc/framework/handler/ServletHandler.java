package ink.zhaibo.mvc.framework.handler;

import ink.zhaibo.mvc.framework.annotations.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @Author zhaibo
 * @Description
 * @Date 2018/6/4
 */
public class ServletHandler {
    /**
     * 保存方法对应的实例
     */
    public Object controller;
    /**
     * 保存映射的方法
     */
    public Method method;
    public Pattern pattern;
    /**
     * 参数顺序
     */
    public Map<String, Object> paramIndexMapping;

    /**
     * 构造handler
     *
     * @param controller
     * @param method
     * @param pattern
     */
    public ServletHandler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;

        this.paramIndexMapping = new HashMap<String, Object>();
        this.putParamIndexMapping(method);
    }


    private void putParamIndexMapping(Method method) {
        // 提取方法中加了注解的参数
        Annotation[][] pa = method.getParameterAnnotations();

        for (int i = 0; i < pa.length; i++) {
            for (Annotation annotation : pa[i]) {
                String paramName = ((RequestParam) annotation).value();
                if (!"".equals(paramName.trim())) {
                    paramIndexMapping.put(paramName, i);
                }
            }
        }

        //提取参数中的request 和response
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                paramIndexMapping.put(type.getName(), i);
            }
        }
    }
}

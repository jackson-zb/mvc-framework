package ink.zhaibo.mvc.framework.servlet;

import ink.zhaibo.mvc.framework.annotations.*;
import ink.zhaibo.mvc.framework.handler.ServletHandler;
import ink.zhaibo.mvc.framework.utils.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {

    /**
     * 属性文件配置
     */
    private Properties contextConfig = new Properties();

    /**
     * 类名集合
     */
    private List<String> classNames = new ArrayList<String>();

    /**
     * IOC容器
     */
    private Map<String, Object> iocContainer = new HashMap<String, Object>();

    /**
     * handler
     */
    private List<ServletHandler> handlerMapping = new ArrayList<ServletHandler>();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("doGet 进来1");
        ServletHandler handler = this.getServletHandler(req);
        Map<String, String[]> parameterMap = req.getParameterMap();
        handler.paramIndexMapping.putAll(parameterMap);
        if (handler == null || handler.method == null) {
            resp.getWriter().write("404 NOT FOUND !");
        }

        List<String> paramList = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : handler.paramIndexMapping.entrySet()) {
            paramList.add(entry.getValue().toString());
        }
        try {
            //参数值处理
            Object invoke = handler.method.invoke(handler.controller,paramList.toArray());
            resp.getWriter().write(invoke.toString());
        } catch (Exception e) {
            resp.getWriter().write("500 " + e);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletHandler handler = this.getServletHandler(req);

        if (handler == null || handler.method == null) {
            resp.getWriter().write("404 NOT FOUND !");
        }

        try {
            Object invoke = handler.method.invoke(handler.paramIndexMapping);
            resp.getWriter().write(invoke.toString());
        } catch (Exception e) {
            resp.getWriter().write("500");
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("开始初始化");

        //1、加载配置
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        this.loadConfig(contextConfigLocation);

        //2、扫描所有相关的类
        this.scanClasses(contextConfig.getProperty("scanPackage"));

        //3、初始化所有相关类
        this.doInstance();

        //4、自动注入
        this.autowired();

        //5、初始化handlerMapping
        this.initHandlerMapping();
    }

    private ServletHandler getServletHandler(HttpServletRequest request) {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        for (ServletHandler handler : handlerMapping) {
            Matcher matcher = handler.pattern.matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }

    /**
     * 初始化handlerMapping
     */
    private void initHandlerMapping() {
        if (iocContainer.isEmpty()) {
            return;
        }

        StringBuilder urlSb = new StringBuilder();
        StringBuilder tempSb = new StringBuilder();

        for (Map.Entry<String, Object> entry : iocContainer.entrySet()) {
            urlSb = tempSb;

            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            urlSb = urlSb.append("/").append(requestMapping.value());

            Method[] methods = clazz.getMethods();

            for (Method method : methods) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                urlSb = urlSb.append(methodRequestMapping.value().replaceAll("/+", "/"));
                //TODO
                handlerMapping.add(new ServletHandler(entry.getValue(), method, Pattern.compile(urlSb.toString())));
            }

            System.out.println("Mapping");

        }
    }

    /**
     * 自动注入
     */
    private void autowired() {
        if (iocContainer.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocContainer.entrySet()) {
            //获取所有属性
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                //取出加Autowired的属性
                if (field.isAnnotationPresent(Autowired.class)) {
                    Autowired autowired = field.getAnnotation(Autowired.class);
                    String beanName = autowired.value();
                    if ("".equals(beanName.trim())) {
                        beanName = field.getName();
                    }

                    //打破封装
                    field.setAccessible(true);

                    try {
                        field.set(entry.getValue(), iocContainer.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }


    /**
     * 实例化所有添加相关注解的类
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    String beanName = StringUtils.lowerFirstCase(clazz.getName());

                    //类名称首字母小写作为key
                    iocContainer.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    String beanName = "";
                    Service service = clazz.getAnnotation(Service.class);
                    String value = service.value();

                    // 默认类名称首字母小写作为key
                    // value不为空,使用value

                    if ("".equals(value.trim())) {
                        beanName = StringUtils.lowerFirstCase(clazz.getName());
                    } else {
                        beanName = value;
                    }
                    iocContainer.put(beanName, clazz.newInstance());
                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据配置里的包路径扫描所有相关类
     *
     * @param scanPackage
     */
    private void scanClasses(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replace(".", "/"));
        File classDir = new File(url.getFile().replace("%20", " "));

        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                // 递归取出类名称
                scanClasses(scanPackage + "." + file.getName());
            } else {
                String className = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }


    /**
     * 从web.xml中获取配置文件路径，加载
     *
     * @param contextConfigLocation
     */
    private void loadConfig(String contextConfigLocation) {
       /*
            Class<? extends DispatcherServlet> aClass = this.getClass();
            ClassLoader classLoader = aClass.getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream(contextConfigLocation);
            InputStream is = this.getServletContext().getResourceAsStream(contextConfigLocation);//这里不能用
        */

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

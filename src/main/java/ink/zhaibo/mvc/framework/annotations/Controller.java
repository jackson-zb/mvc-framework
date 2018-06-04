package ink.zhaibo.mvc.framework.annotations;

import java.lang.annotation.*;

/**
 * @Date 2018/5/31
 * @Description 自定义注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
    String value() default "";
}

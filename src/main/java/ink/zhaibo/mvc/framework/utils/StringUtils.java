package ink.zhaibo.mvc.framework.utils;

/**
 * @Author zhaibo
 * @Description 字符串工具类
 * @Date 2018/5/31
 */
public class StringUtils {
    /**
     * 转换首字母为小写
     */
    public static String lowerFirstCase(String className) {
        if ("".equals(className.trim())) {
            return null;
        }
        byte[] bytes = className.getBytes();
        bytes[0] += 32;
        return bytes.toString();
    }
}

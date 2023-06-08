package com.flipped.mybatis.audit.context;

import java.util.HashMap;
import java.util.Map;

public class AuditFiledContext {

    private static final ThreadLocal<String> CURRENT_USER_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> infos;

    static {
        Map<String, String> map = new HashMap<>();
        infos = new ThreadLocal<>();
        infos.set(map);
    }

    public static final String UPDATE_BY_COLUMN = "update_by_column";
    public static final String UPDATE_BY_PROPERTY = "update_by_property";

    public static final String UPDATE_TIME_COLUMN = "update_time_column";
    public static final String UPDATE_TIME_PROPERTY = "update_time_property";

    public static final String CREATE_BY_COLUMN = "create_by_column";
    public static final String CREATE_BY_PROPERTY = "create_by_property";
    public static final String CREATE_TIME_COLUMN = "create_time_column";
    public static final String CREATE_TIME_PROPERTY = "create_time_property";

    private AuditFiledContext() {
    }


    public static void set(String key, String value) {
        infos.get().put(key, value);
    }

    public static String get(String key) {
        return infos.get().get(key);
    }


    public static void setCurrentUser(String user) {
        CURRENT_USER_THREAD_LOCAL.set(user);
    }

    public static String getUserName() {
        return CURRENT_USER_THREAD_LOCAL.get();

    }

    public static void clear() {
        infos.get().clear();
    }
}

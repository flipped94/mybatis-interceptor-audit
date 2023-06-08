package com.flipped.mybatis.audit.interceptor;

import com.flipped.mybatis.audit.annotation.CreateBy;
import com.flipped.mybatis.audit.annotation.CreateTime;
import com.flipped.mybatis.audit.annotation.UpdateBy;
import com.flipped.mybatis.audit.annotation.UpdateTime;
import com.flipped.mybatis.audit.context.AuditFiledContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 自定义 Mybatis 插件，自动设置 createTime, createBy, updateTime, updateBy 的值
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class AuditingInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        if (target instanceof Executor) {
            Object[] args = invocation.getArgs();
            MappedStatement mappedStatement = (MappedStatement) args[0];
            Object parameter = args[1];
            setAuditValue(parameter, mappedStatement.getSqlCommandType());
        }
        if (target instanceof StatementHandler) {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            replaceSQL(statementHandler);
        }

        Object result = invocation.proceed();
        AuditFiledContext.clear();
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private void setAuditValue(Object parameter, SqlCommandType sqlCommandType) throws IllegalAccessException {
        Class<?> aClass = parameter.getClass();
        List<Field> declaredFields = new ArrayList<>();
        declaredFields.addAll(List.of(aClass.getDeclaredFields()));
        declaredFields.addAll(List.of(aClass.getSuperclass().getDeclaredFields()));

        for (Field field : declaredFields) {
            if (SqlCommandType.INSERT.equals(sqlCommandType)) { // insert 语句插入 createBy
                if (field.getAnnotation(CreateBy.class) != null) {
                    field.setAccessible(true);
                    field.set(parameter, AuditFiledContext.getUserName());
                    String column = field.getAnnotation(CreateBy.class).column();
                    AuditFiledContext.set(AuditFiledContext.CREATE_BY_COLUMN, column);
                    AuditFiledContext.set(AuditFiledContext.CREATE_BY_PROPERTY, field.getName());
                }

                if (field.getAnnotation(CreateTime.class) != null) { // insert 语句插入 createTime
                    field.setAccessible(true);
                    field.set(parameter, new Date());
                    String column = field.getAnnotation(CreateTime.class).column();
                    AuditFiledContext.set(AuditFiledContext.CREATE_TIME_COLUMN, column);
                    AuditFiledContext.set(AuditFiledContext.CREATE_TIME_PROPERTY, field.getName());
                }
            } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
                if (field.getAnnotation(UpdateBy.class) != null) { // update 语句插入 updateBy
                    field.setAccessible(true);
                    field.set(parameter, AuditFiledContext.getUserName());
                    String column = field.getAnnotation(UpdateBy.class).column();
                    AuditFiledContext.set(AuditFiledContext.UPDATE_BY_COLUMN, column);
                    AuditFiledContext.set(AuditFiledContext.UPDATE_BY_PROPERTY, field.getName());
                }
                if (field.getAnnotation(UpdateTime.class) != null) { // update 语句插入 updateTime
                    field.setAccessible(true);
                    field.set(parameter, new Date());
                    String column = field.getAnnotation(UpdateTime.class).column();
                    AuditFiledContext.set(AuditFiledContext.UPDATE_TIME_COLUMN, column);
                    AuditFiledContext.set(AuditFiledContext.UPDATE_TIME_PROPERTY, field.getName());
                }
            }
        }
    }

    private void replaceSQL(StatementHandler statementHandler) throws NoSuchFieldException, IllegalAccessException {
        MetaObject metaObject = MetaObject.forObject(
                statementHandler,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                new DefaultReflectorFactory()
        );
        // 获取成员变量 mappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        BoundSql boundSql = statementHandler.getBoundSql();
        String originSQL = boundSql.getSql();
        Field field = boundSql.getClass().getDeclaredField("sql");
        String newSql = generateNewSql(mappedStatement.getSqlCommandType(), originSQL);
        field.setAccessible(true);
        field.set(boundSql, newSql);
        addParameterMapping(mappedStatement, boundSql);
    }

    private String generateNewSql(SqlCommandType sqlCommandType, String originalSQL) {
        String newSql = "";
        if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
            int i = originalSQL.indexOf(" set ");
            newSql = originalSQL.substring(0, i + 4) + " " + AuditFiledContext.get(AuditFiledContext.UPDATE_BY_COLUMN) + "=?, "
                    + AuditFiledContext.get(AuditFiledContext.UPDATE_TIME_COLUMN) + "=?, " + originalSQL.substring(i + 5);
        } else if (SqlCommandType.INSERT.equals(sqlCommandType)) {
            int i = originalSQL.indexOf("(");
            String addInsertFiled = originalSQL.substring(0, i + 1) + AuditFiledContext.get(AuditFiledContext.CREATE_BY_COLUMN) + ", "
                    + AuditFiledContext.get(AuditFiledContext.CREATE_TIME_COLUMN) + ", " + originalSQL.substring(i + 1);
            int i1 = addInsertFiled.indexOf("values(");
            newSql = addInsertFiled.substring(0, i1 + 7) + "?, ?, " + addInsertFiled.substring(i1 + 7);
        }
        return newSql;
    }

    private void addParameterMapping(MappedStatement mappedStatement, BoundSql boundSql) {
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (mappedStatement.getSqlCommandType().equals(SqlCommandType.UPDATE)) {
            parameterMappings.add(0, new ParameterMapping.Builder(mappedStatement.getConfiguration(), AuditFiledContext.get(AuditFiledContext.UPDATE_TIME_PROPERTY), Date.class).build());
            parameterMappings.add(0, new ParameterMapping.Builder(mappedStatement.getConfiguration(), AuditFiledContext.get(AuditFiledContext.UPDATE_BY_PROPERTY), String.class).build());
        } else if (mappedStatement.getSqlCommandType().equals(SqlCommandType.INSERT)) {
            parameterMappings.add(0, new ParameterMapping.Builder(mappedStatement.getConfiguration(), AuditFiledContext.get(AuditFiledContext.CREATE_TIME_PROPERTY), Date.class).build());
            parameterMappings.add(0, new ParameterMapping.Builder(mappedStatement.getConfiguration(), AuditFiledContext.get(AuditFiledContext.CREATE_BY_PROPERTY), String.class).build());
        }
    }
}

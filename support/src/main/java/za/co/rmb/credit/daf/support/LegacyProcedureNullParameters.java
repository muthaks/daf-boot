package za.co.rmb.credit.daf.support;

import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stored-procedure IN parameters with a null value behave as they did on Hibernate 5.
 *
 * <p>The legacy persistence unit did not enable passing nulls to procedures, so Hibernate 5 left a null IN
 * parameter unbound and the call failed at execution ("The value is not set for the parameter number n");
 * the repositories catch that and report failure (for example {@code archiveDocument} answers
 * {@code success: false} for a missing document id). Hibernate 6 and later always bind SQL NULL, so the
 * procedure would run. This wraps the DataSource so that a procedure call ({@code prepareCall}) with a
 * parameter set to null fails at execution the same way. Nothing else is affected; none of DAF's
 * procedures declares a default value, so there is no case where an unbound parameter ever succeeded.</p>
 */
public final class LegacyProcedureNullParameters implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource dataSource && !Proxy.isProxyClass(bean.getClass())) {
            return proxy(DataSource.class, dataSource, (target, method, args) -> {
                Object result = invoke(target, method, args);
                return result instanceof Connection connection ? wrapConnection(connection) : result;
            });
        }
        return bean;
    }

    private static Connection wrapConnection(Connection connection) {
        return proxy(Connection.class, connection, (target, method, args) -> {
            Object result = invoke(target, method, args);
            if ("prepareCall".equals(method.getName()) && result instanceof CallableStatement statement) {
                return wrapCall(statement);
            }
            return result;
        });
    }

    private static CallableStatement wrapCall(CallableStatement statement) {
        Set<Integer> unbound = new TreeSet<>();
        return proxy(CallableStatement.class, statement, (target, method, args) -> {
            String name = method.getName();
            if (args != null && args.length >= 1 && args[0] instanceof Integer index && name.startsWith("set")) {
                boolean isNull = "setNull".equals(name) || (args.length >= 2 && args[1] == null);
                if (isNull) {
                    unbound.add(index);
                    return null;
                }
                unbound.remove(index);
            }
            if (name.startsWith("execute") && !unbound.isEmpty()) {
                throw new SQLException("The value is not set for the parameter number " + unbound.iterator().next() + ".");
            }
            if ("clearParameters".equals(name)) {
                unbound.clear();
            }
            return invoke(target, method, args);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, T target, Handler<T> handler) {
        InvocationHandler invocation = (proxy, method, args) -> handler.handle(target, method, args);
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, invocation);
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @FunctionalInterface
    private interface Handler<T> {
        Object handle(T target, Method method, Object[] args) throws Throwable;
    }
}

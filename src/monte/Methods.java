/*
 * @(#)Methods.java
 *
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

package monte;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Methods contains convenience methods for method invocations using
 * java.lang.reflect.
 *
 * @author  Werner Randelshofer
 * @version $Id: Methods.java 299 2013-01-03 07:40:18Z werner $
 */

@SuppressWarnings("unchecked")
public class Methods {
    /**
     * Prevent instance creation.
     */
    private Methods () {
    }

    /**
     * Invokes the specified parameterless method if it exists.
     *
     * @param clazz The class on which to invoke the method.
     * @param methodName The name of the method.
     * @param types The parameter types.
     * @param values The parameter values.
     * @return The return value of the method.
     * @return NoSuchMethodException if the method does not exist or is not accessible.
     */
    public static Object invokeStatic(Class clazz, String methodName, Class[] types, Object[] values)
    throws NoSuchMethodException {
        try {
            Method method =  clazz.getMethod(methodName,  types);
            Object result = method.invoke(null, values);
            return result;
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(methodName+" is not accessible");
        } catch (InvocationTargetException e) {
            // The method is not supposed to throw exceptions
            throw new InternalError(e.getMessage());
        }
    }

    /**
     * Invokes the specified setter method if it exists.
     *
     * @param obj The object on which to invoke the method.
     * @param methodName The name of the method.
     */
    public static Object invoke(Object obj, String methodName, Class clazz, Object newValue)
    throws NoSuchMethodException {
        try {
            Method method =  obj.getClass().getMethod(methodName,  new Class[] { clazz } );
            return method.invoke(obj, new Object[] { newValue});
        } catch (IllegalAccessException e) {
            throw new NoSuchMethodException(methodName+" is not accessible");
        } catch (InvocationTargetException e) {
            // The method is not supposed to throw exceptions
            throw new InternalError(e.getMessage());
        }
    }

}

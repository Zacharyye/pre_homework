package com.zachary.pre_homework02.function;


import static com.zachary.pre_homework02.util.ExceptionUtils.wrapThrowable;

@FunctionalInterface
public interface ThrowableSupplier<T> {

    /**
     * Applies this function to the given argument.
     *
     * @return the supplied result
     * @throws Throwable if met with any error
     */
    T get() throws Throwable;

    /**
     * Executes {@link ThrowableSupplier}
     *
     * @param supplier {@link ThrowableSupplier}
     * @param <T>      the supplied type
     * @return the result after execution
     * @throws RuntimeException
     */
    static <T> T execute(ThrowableSupplier<T> supplier) throws RuntimeException {
        return execute(supplier, RuntimeException.class);
    }

    static <T, E extends Throwable> T execute(ThrowableSupplier<T> supplier, Class<E> errorType) throws E {
        T result = null;
        try {
            result = supplier.get();
        } catch (Throwable e) {
            throw wrapThrowable(e, errorType);
        }
        return result;
    }
}


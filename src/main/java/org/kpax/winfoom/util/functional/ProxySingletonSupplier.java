package org.kpax.winfoom.util.functional;

import org.kpax.winfoom.proxy.listener.StopListener;

public class ProxySingletonSupplier<T> extends SingletonSupplier<T> implements StopListener {
    /**
     * Constructor.
     *
     * @param supplier the not null supplier
     */
    public ProxySingletonSupplier(RuntimeExceptionSupplier<T> supplier) {
        super(supplier);
    }

    @Override
    public void onStop() {
        reset();
    }
}

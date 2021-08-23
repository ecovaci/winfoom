package org.kpax.winfoom.util.functional;

import java.security.PrivilegedExceptionAction;

public interface PrivilegedActionWrapper extends PrivilegedExceptionAction<Object> {

    void execute() throws Exception;

    @Override
    default Object run() throws Exception {
        execute();
        return null;
    }
}

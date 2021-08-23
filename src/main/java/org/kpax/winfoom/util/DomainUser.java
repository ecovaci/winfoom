package org.kpax.winfoom.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.kpax.winfoom.annotation.NotNull;

import java.util.Locale;

@Getter
@ToString
@RequiredArgsConstructor
public final class DomainUser {

    private final String username;
    private final String domain;

    public static DomainUser from(@NotNull String domainUsername) {
        int backslashIndex = domainUsername.indexOf('\\');
        return new DomainUser(backslashIndex > -1 ?
                domainUsername.substring(backslashIndex + 1) : domainUsername,
                backslashIndex > -1 ?
                        domainUsername.substring(0, backslashIndex).toUpperCase(Locale.ROOT) : null);
    }

    public static String extractUsername(String domainUsername) {
        if (domainUsername != null) {
            int backslashIndex = domainUsername.indexOf('\\');
            return backslashIndex > -1 ?
                    domainUsername.substring(backslashIndex + 1) : domainUsername;
        } else {
            return null;
        }
    }

}

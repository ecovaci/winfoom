package org.kpax.winfoom.proxy;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kpax.winfoom.config.ProxyConfig;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ManualAuthCredentialsProviderTest {

    @Mock
    private ProxyConfig proxyConfig;

    @InjectMocks
    private ManualAuthCredentialsProvider underTest;

    @Test
    void setCredentials_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> underTest.setCredentials(null, null));
    }

    @Test
    void getCredentials_isNtlm_returnsNTCredentials() {
        when(proxyConfig.isNtlm()).thenReturn(true);
        when(proxyConfig.getProxyUsername()).thenReturn("username");
        Credentials actualCredentials = underTest.getCredentials(AuthScope.ANY);
        assertThat(actualCredentials).isInstanceOf(NTCredentials.class);
    }

    @Test
    void getCredentials_nonNtlm_returnsUsernamePasswordCredentials() {
        when(proxyConfig.isNtlm()).thenReturn(false);
        when(proxyConfig.getProxyUsername()).thenReturn("username");
        Credentials actualCredentials = underTest.getCredentials(AuthScope.ANY);
        assertThat(actualCredentials).isInstanceOf(UsernamePasswordCredentials.class);
    }

    @Test
    void clear() {
        when(proxyConfig.isNtlm()).thenReturn(true);
        when(proxyConfig.getProxyUsername()).thenReturn("username");
        Credentials existentCredentials = underTest.getCredentials(AuthScope.ANY);
        underTest.clear();
        Credentials currentCredentials = underTest.getCredentials(AuthScope.ANY);
        assertThat(currentCredentials).isNotSameAs(existentCredentials);
    }
}
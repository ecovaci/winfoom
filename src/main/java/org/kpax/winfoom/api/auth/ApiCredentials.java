/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.kpax.winfoom.api.auth;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.http.auth.Credentials;

import java.security.Principal;

/**
 * Manage the API server credentials.
 */
public class ApiCredentials implements Credentials {

    private final Principal principal;

    public ApiCredentials(String apiUserPassword) {
        principal = new BasicUserPrincipal(apiUserPassword);
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public String getPassword() {
        return null;
    }
}

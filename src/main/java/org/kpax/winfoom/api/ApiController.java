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

package org.kpax.winfoom.api;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.Credentials;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.kpax.winfoom.api.auth.ApiCredentials;
import org.kpax.winfoom.api.dto.ConfigDto;
import org.kpax.winfoom.api.dto.SettingsDto;
import org.kpax.winfoom.api.json.Views;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.proxy.ProxyController;
import org.kpax.winfoom.proxy.ProxyExecutorService;
import org.kpax.winfoom.proxy.ProxyValidator;
import org.kpax.winfoom.util.BeanUtils;
import org.kpax.winfoom.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Open an API server and map various request handlers.
 */
@Slf4j
@Profile({"!gui & !test"})
@Component
public class ApiController implements AutoCloseable {

    private static final int SHUTDOWN_GRACE_PERIOD = 1000;

    private HttpServer apiServer;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private ProxyController proxyController;

    @Autowired
    private ProxyExecutorService executorService;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @PostConstruct
    private void init() throws IOException {
        Credentials credentials = new ApiCredentials(proxyConfig.getApiToken());
        logger.info("Register API request handlers");
        apiServer = ServerBootstrap.bootstrap().setListenerPort(proxyConfig.getApiPort()).
                registerHandler("/start",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'start' command received");
                                try {
                                    proxyConfig.validate();
                                    proxyController.start();
                                    response.setEntity(new StringEntity("The local proxy server has been started"));
                                } catch (InvalidProxySettingsException e) {
                                    response.setEntity(new StringEntity("Invalid configuration: " + e.getMessage()));
                                } catch (Exception e) {
                                    logger.error("Error on starting local proxy server", e);
                                    response.setEntity(new StringEntity("Failed to start the local proxy: " + e.getMessage()));
                                }
                            }
                        }).
                registerHandler("/stop",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'stop' command received");
                                if (proxyController.isRunning()) {
                                    try {
                                        proxyController.stop();
                                        response.setEntity(new StringEntity("The local proxy server has been stopped"));
                                    } catch (Exception e) {
                                        logger.error("Error on stopping local proxy server", e);
                                        response.setEntity(new StringEntity("Failed to stop the local proxy: " + e.getMessage()));
                                    }
                                } else {
                                    response.setEntity(new StringEntity("Already stopped, nothing to do"));
                                }
                            }
                        }).
                registerHandler("/status",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'status' command received");
                                response.setEntity(new StringEntity(String.format("The local proxy server is %s",
                                        proxyController.isRunning() ? "up" : "stopped")));
                            }
                        }).
                registerHandler("/validate",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'validate' command received");
                                boolean running = proxyController.isRunning();
                                if (running) {
                                    try {
                                        applicationContext.getBean(ProxyValidator.class).testProxy();
                                        response.setEntity(new StringEntity("The configuration is valid"));
                                    } catch (InvalidProxySettingsException e) {
                                        logger.debug("Invalid proxy settings", e);
                                        response.setEntity(new StringEntity("The configuration is invalid: " + e.getMessage()));
                                    } catch (IOException e) {
                                        logger.error("Failed to validate proxy settings", e);
                                        response.setEntity(new StringEntity("Error on validation the configuration: " + e.getMessage()));
                                    }
                                } else {
                                    response.setEntity(new StringEntity("The local proxy server is down, you need to start it before validating configuration"));
                                }
                            }
                        }).
                registerHandler("/autodetect",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'autodetect' command received");
                                if (systemConfig.isApiReadOnly()) {
                                    response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                                    response.setEntity(new StringEntity("Forbidden: Modifying configuration is disabled"));
                                    return;
                                }
                                if (proxyController.isStopped()) {
                                    try {
                                        boolean result = proxyConfig.autoDetect();
                                        if (result) {
                                            response.setEntity(new StringEntity("Autodetect succeeded"));
                                        } else {
                                            response.setEntity(new StringEntity("No proxy configuration found on your system"));
                                        }
                                    } catch (Exception e) {
                                        logger.error("Error on autodetect proxy settings", e);
                                        response.setEntity(new StringEntity("Error on auto-detecting the configuration: " + e.getMessage()));
                                    }
                                } else {
                                    response.setEntity(new StringEntity("The local proxy server is up, you need to stop it before auto-detecting configuration"));
                                }
                            }
                        }).
                registerHandler("/config",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'config get' command received");
                                try {
                                    response.setEntity(new StringEntity(new ObjectMapper().
                                            configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false).
                                            writerWithDefaultPrettyPrinter().
                                            withView(Views.getView(proxyConfig)).
                                            writeValueAsString(proxyConfig)));
                                } catch (Exception e) {
                                    logger.error("Error on serializing proxy configuration", e);
                                    response.setEntity(new StringEntity("Failed to get proxy configuration: " + e.getMessage()));
                                }
                            }

                            @Override
                            public void doPost(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'config post' command received");
                                if (systemConfig.isApiReadOnly()) {
                                    response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                                    response.setEntity(new StringEntity("Forbidden: Modifying configuration is disabled"));
                                    return;
                                }
                                boolean running = proxyController.isRunning();
                                if (running) {
                                    response.setEntity(new StringEntity("The local proxy server is up, you need to stop it before applying configuration"));
                                } else {
                                    if (request instanceof BasicHttpEntityEnclosingRequest) {
                                        BasicHttpEntityEnclosingRequest entityEnclosingRequest = (BasicHttpEntityEnclosingRequest) request;
                                        try {
                                            String json = IOUtils.toString(entityEnclosingRequest.getEntity().getContent(), StandardCharsets.UTF_8);
                                            ConfigDto configDto = new ObjectMapper().readValue(json, ConfigDto.class);
                                            configDto.validate();
                                            BeanUtils.copyProperties(JsonUtils.getFieldNames(json), configDto, proxyConfig);
                                            response.setEntity(new StringEntity("Proxy configuration changed"));
                                        } catch (IOException e) {
                                            logger.error("Error on parsing JSON", e);
                                            response.setEntity(new StringEntity("Failed to parse JSON: " + e.getMessage()));
                                        } catch (InvalidProxySettingsException e) {
                                            logger.error("Invalid JSON", e);
                                            response.setEntity(new StringEntity("Invalid JSON: " + e.getMessage()));
                                        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                                            logger.error("Error on applying proxy configuration", e);
                                            response.setEntity(new StringEntity("Failed to changed proxy configuration: " + e.getMessage()));
                                        }
                                    } else {
                                        response.setEntity(new StringEntity("Failed to changed proxy configuration: no JSON found"));
                                    }
                                }
                            }
                        }).
                registerHandler("/settings",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'settings' command received");
                                try {
                                    response.setEntity(new StringEntity(new ObjectMapper().
                                            configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false).
                                            writerWithDefaultPrettyPrinter().
                                            withView(Views.getSettingsView()).
                                            writeValueAsString(proxyConfig)));
                                } catch (Exception e) {
                                    logger.error("Error on serializing proxy settings", e);
                                    response.setEntity(new StringEntity("Failed to get proxy settings: " + e.getMessage()));
                                }
                            }

                            @Override
                            public void doPost(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'settings post' command received");
                                if (systemConfig.isApiReadOnly()) {
                                    response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                                    response.setEntity(new StringEntity("Forbidden: Modifying settings is disabled"));
                                    return;
                                }
                                boolean running = proxyController.isRunning();
                                if (running) {
                                    response.setEntity(new StringEntity("The local proxy server is up, you need to stop it before changing settings"));
                                } else {
                                    if (request instanceof BasicHttpEntityEnclosingRequest) {
                                        BasicHttpEntityEnclosingRequest entityEnclosingRequest = (BasicHttpEntityEnclosingRequest) request;
                                        try {
                                            String json = IOUtils.toString(entityEnclosingRequest.getEntity().getContent(), StandardCharsets.UTF_8);
                                            SettingsDto settingsDto = new ObjectMapper().readValue(json, SettingsDto.class);
                                            settingsDto.validate();
                                            BeanUtils.copyProperties(JsonUtils.getFieldNames(json), settingsDto, proxyConfig);
                                            response.setEntity(new StringEntity("Proxy settings changed"));
                                        } catch (IOException e) {
                                            logger.error("Error on parsing JSON", e);
                                            response.setEntity(new StringEntity("Failed to parse JSON: " + e.getMessage()));
                                        } catch (InvalidProxySettingsException e) {
                                            logger.error("Invalid JSON", e);
                                            response.setEntity(new StringEntity("Invalid JSON: " + e.getMessage()));
                                        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                                            logger.error("Error on applying proxy settings", e);
                                            response.setEntity(new StringEntity("Failed to change proxy settings: " + e.getMessage()));
                                        }
                                    } else {
                                        response.setEntity(new StringEntity("Failed to changed proxy settings: no JSON found"));
                                    }
                                }
                            }
                        }).
                registerHandler("/shutdown",
                        new GenericHttpRequestHandler(credentials, executorService, systemConfig) {
                            @Override
                            public void doGet(HttpRequest request, HttpResponse response, HttpContext context)
                                    throws IOException {
                                logger.debug("'shutdown' command received");
                                if (systemConfig.isApiDisableShutdown()) {
                                    response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                                    response.setEntity(new StringEntity("Forbidden: Shutdown is disabled"));
                                } else {
                                    new Thread(applicationContext::close).start();
                                    response.setEntity(new StringEntity("Shutdown initiated"));
                                }
                            }
                        }).create();
        apiServer.start();
    }


    @Override
    public void close() {
        logger.info("Stop the api server");
        try {
            try {
                apiServer.awaitTermination(SHUTDOWN_GRACE_PERIOD, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Ignore
            }
            apiServer.shutdown(0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.debug("Error on stopping API server", e);
        }
    }
}

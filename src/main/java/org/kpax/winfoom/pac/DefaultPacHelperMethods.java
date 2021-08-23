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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.kpax.winfoom.pac;

import inet.ipaddr.IPAddressString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.pac.datetime.PacDateTimeUtils;
import org.kpax.winfoom.pac.net.IpAddressMatcher;
import org.kpax.winfoom.pac.net.IpAddresses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Default implementation of a the PAC 'helper functions'.
 */
@Slf4j
@Component
public class DefaultPacHelperMethods implements PacHelperMethodsNetscape, PacHelperMethodsMicrosoft {

    private static final Predicate<InetAddress> isIPv4Predicate = a -> a.getClass() == Inet4Address.class;

    @Autowired
    private SystemConfig systemConfig;

    @Autowired
    private GlobPatternMatcher globPatternMatcher;

    // *************************************************************
    //  Official helper functions.
    // *************************************************************

    @Override
    public boolean isPlainHostName(String host) {
        return !host.contains(".");
    }

    @Override
    public boolean dnsDomainIs(String host, String domain) {
        int dotPos = host.indexOf(".");
        if (dotPos != -1 && dotPos < host.length() - 1) {
            if (host.substring(dotPos).equals(domain)) {
                return true;
            }
            return host.substring(dotPos + 1).equals(domain);
        }
        return false;
    }

    @Override
    public boolean localHostOrDomainIs(String host, String hostdom) {
        if (host.equals(hostdom)) {
            return true;
        }
        return Arrays.stream(hostdom.split("\\.")).
                filter(StringUtils::isNotEmpty).
                findFirst().
                map(host::equals).
                orElse(false);
    }

    @Override
    public boolean isResolvable(String host) {
        try {
            return !IpAddresses.resolve(host, isIPv4Predicate).isEmpty();
        } catch (UnknownHostException ex) {
            logger.debug("Error on resolving host [{}]", host);
            return false;
        }
    }

    @Override
    public String dnsResolve(String host) {
        try {
            List<InetAddress> addresses = IpAddresses.resolve(host, isIPv4Predicate);
            if (!addresses.isEmpty()) {
                return addresses.get(0).getHostAddress();
            }
        } catch (UnknownHostException ex) {
            logger.debug("Error on resolving host [{}]", host);
        }
        // Returning null is what Chrome and Firefox do in this situation
        return null;
    }

    @Override
    public String myIpAddress() {
        try {
            return IpAddresses.primaryIPv4Address.get().getHostAddress();
        } catch (Exception e) {
            logger.warn("Cannot get localhost ip address", e);
            return IpAddresses.LOCALHOST;
        }
    }

    @Override
    public boolean isInNet(String host, String pattern, String mask) {
        final String dnsResolve = dnsResolve(host);
        if (dnsResolve == null) {
            return false;
        }
        return new IPAddressString(pattern + "/" + mask).contains(new IPAddressString(host));
    }

    @Override
    public int dnsDomainLevels(String host) {
        return StringUtils.countMatches(host, '.');
    }

    @Override
    public boolean shExpMatch(String str, String shexp) {
        return globPatternMatcher.toPattern(shexp).matcher(str).matches();
    }

    @Override
    public boolean weekdayRange(Object... args) {
        try {
            return PacDateTimeUtils.isInWeekdayRange(new Date(), args);
        } catch (PacDateTimeUtils.PacDateTimeInputException ex) {
            logger.warn("PAC script error : arguments passed to weekdayRange() function {} are faulty: {}",
                    Arrays.toString(args), ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean dateRange(Object... args) {
        try {
            return PacDateTimeUtils.isInDateRange(new Date(), args);
        } catch (PacDateTimeUtils.PacDateTimeInputException ex) {
            logger.warn("PAC script error : arguments passed to dateRange() function {} are faulty: {}",
                    Arrays.toString(args), ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean timeRange(Object... args) {
        try {
            return PacDateTimeUtils.isInTimeRange(new Date(), args);
        } catch (PacDateTimeUtils.PacDateTimeInputException ex) {
            logger.warn("PAC script error : arguments passed to timeRange() function {} are faulty: {}",
                    Arrays.toString(args), ex.getMessage());
            return false;
        }
    }

    // *************************************************************
    //  Microsoft extensions
    // 
    // *************************************************************

    @Override
    public boolean isResolvableEx(String host) {
        try {
            return !IpAddresses.resolve(host).isEmpty();
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    @Override
    public String dnsResolveEx(String host) {
        try {
            List<InetAddress> addresses = IpAddresses.resolve(host);
            if (!addresses.isEmpty()) {
                if (addresses.size() > 1) {
                    addresses.sort(IpAddresses.addressComparator(systemConfig.isPreferIPv6Addresses()));
                }
                return addresses.get(0).getHostAddress();
            }
        } catch (UnknownHostException ex) {
            logger.debug("Error on resolving host [{}]", host);
        }
        return "";
    }

    @Override
    public String myIpAddressEx() {
        try {
            InetAddress[] addresses = IpAddresses.allPrimaryAddresses.get();
            return Arrays.stream(addresses).
                    sorted(IpAddresses.addressComparator(systemConfig.isPreferIPv6Addresses())).
                    map(InetAddress::getHostAddress).
                    collect(Collectors.joining(";"));
        } catch (Exception e) {
            logger.warn("Cannot get localhost ip addresses", e);
            return IpAddresses.LOCALHOST;
        }
    }

    @Override
    public String sortIpAddressList(String ipAddressList) {
        if (StringUtils.isEmpty(ipAddressList)) {
            return "";
        }

        // We convert to InetAddress (because we know how to sort
        // those) but at the same time we have to preserve the way
        // the original input was represented and return in the same
        // format.
        TreeMap<InetAddress, String> addresses = new TreeMap<>(IpAddresses.IPv6_FIRST_TOTAL_ORDERING_COMPARATOR);
        for (String host : ipAddressList.split(";")) {
            try {
                addresses.put(InetAddress.getByName(host.trim()), host);
            } catch (UnknownHostException ex) {
                return "";
            }
        }
        return addresses.values().stream().map(String::trim).collect(Collectors.joining(";"));
    }

    @Override
    public String getClientVersion() {
        return "1.0";
    }

    @Override
    public boolean isInNetEx(String ipAddress, String ipPrefix) {
        try {
            return new IpAddressMatcher(ipPrefix).matches(ipAddress);
        } catch (UnknownHostException e) {
            return false;
        }
    }


    // *************************************************************
    //  Utility functions.
    // 
    //  Other functions - not defined in PAC spec, but still 
    //  exposed to the JavaScript engine.
    // *************************************************************

    public void alert(String message) {
        logger.debug("PAC script says: {}", message);
    }


}

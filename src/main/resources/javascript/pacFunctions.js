/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

(function(self) {
    this['isPlainHostName'] = function(host) {
        return self.isPlainHostName(host);
    }

    this['dnsDomainIs'] = function(host, domain) {
        return self.dnsDomainIs(host, domain);
    }

    this['localHostOrDomainIs'] = function(host, hostdom) {
        return self.localHostOrDomainIs(host, hostdom);
    }

    this['isResolvable'] = function(host) {
        return self.isResolvable(host);
    }

    this['isInNet'] = function(host, pattern, mask) {
        return self.isInNet(host, pattern, mask);
    }

    this['dnsResolve'] = function(host) {
        return String(self.dnsResolve(host));
    }

    this['myIpAddress'] = function() {
        return String(self.myIpAddress());
    }

    this['dnsDomainLevels'] = function(host) {
        return Number(self.dnsDomainLevels(host));
    }

    this['shExpMatch'] = function(str, shexp) {
        return self.shExpMatch(str, shexp);
    }

    this['weekdayRange'] = function(wd1, wd2, gmt) {
        return self.weekdayRange(wd1, wd2, gmt);
    }

    this['dateRange'] = function(day1, month1, year1, day2, month2, year2, gmt) {
        return self.dateRange(day1, month1, year1, day2, month2, year2, gmt);
    }

    this['timeRange'] = function(hour1, min1, sec1, hour2, min2, sec2, gmt) {
        return self.timeRange(hour1, min1, sec1, hour2, min2, sec2, gmt);
    }

    this['isResolvableEx'] = function(host) {
        return self.isResolvableEx(host);
    }

    this['isInNetEx'] = function(host, ipPrefix) {
        return self.isInNetEx(host, ipPrefix);
    }

    this['dnsResolveEx'] = function(host) {
        return String(self.dnsResolveEx(host));
    }

    this['myIpAddressEx'] = function() {
        return String(self.myIpAddressEx());
    }

    this['sortIpAddressList'] = function(ipAddressList) {
        return String(self.sortIpAddressList(ipAddressList));
    }

    this['getClientVersion'] = function() {
        return String(self.getClientVersion());
    }

    this['alert'] = function(txt) {
        return self.alert(txt);
    }

})
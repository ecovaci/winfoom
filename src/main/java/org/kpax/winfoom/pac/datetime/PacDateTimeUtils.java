/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

package org.kpax.winfoom.pac.datetime;


import org.kpax.winfoom.pac.PacHelperMethodsNetscape;

import java.util.*;

/**
 * Methods and constants useful in PAC script evaluation, specifically
 * date/time related.
 *
 * @author lbruun
 */
public class PacDateTimeUtils {

    /**
     * List of valid weekday names as used in the Netscape specification.
     * <p>
     * Content: {@code  SUN  MON  TUE  WED  THU  FRI  SAT}
     */
    public final static List<String> WEEKDAY_NAMES = List.of("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");

    /**
     * List of valid month names as used in the Netscape specification.
     * <p>
     * Content: {@code JAN  FEB  MAR  APR  MAY  JUN  JUL  AUG  SEP  OCT  NOV  DEC}
     */
    public final static List<String> MONTH_NAMES = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

    private static final TimeZone UTC_TIME = TimeZone.getTimeZone("UTC");


    /**
     * Evaluates if now is within a weekday range. Method arguments are as described
     * for {@link PacHelperMethodsNetscape#weekdayRange(Object...) }
     *
     * @param now
     * @param args
     * @return true if within range
     * @throws PacDateTimeInputException if arguments were invalid
     */
    public static boolean isInWeekdayRange(Date now, Object... args) throws PacDateTimeInputException {
        ParamsInfo paramsInfo = getParamsInfo(args);

        if (!(paramsInfo.count >= 1 && paramsInfo.count <= 2)) {
            throw new PacDateTimeInputException("invalid number of arguments");
        }
        final int wdNumMin;
        final int wdNumMax;
        if (paramsInfo.count == 1) {
            wdNumMin = getWeekday(args[0].toString());
            wdNumMax = wdNumMin;
        } else {
            wdNumMin = getWeekday(args[0].toString());
            wdNumMax = getWeekday(args[1].toString());
        }

        Calendar cal = getCalendar(now, paramsInfo.useGMT);
        int wdNum = cal.get(Calendar.DAY_OF_WEEK);

        if (wdNumMin <= wdNumMax) {
            return (wdNum >= wdNumMin && wdNum <= wdNumMax);
        } else {
            return (wdNum >= wdNumMin || wdNum <= wdNumMax);
        }
    }


    /**
     * Evaluates if now is within a time range. Method arguments are as described
     * for {@link PacHelperMethodsNetscape#timeRange(Object...) }
     *
     * @param now
     * @param args
     * @return true if within range
     * @throws PacDateTimeInputException if arguments were invalid
     */
    public static boolean isInTimeRange(Date now, Object... args) throws PacDateTimeInputException {
        ParamsInfo paramsInfo = getParamsInfo(args);

        if (!(paramsInfo.count >= 1 && paramsInfo.count <= 6) || paramsInfo.count == 5 || paramsInfo.count == 3) {
            throw new PacDateTimeInputException("invalid number of arguments");
        }

        TimeRange.TimeRangeBuilder builder = TimeRange.getBuilder();

        if (paramsInfo.count == 1) {
            builder.withHourMinMax(getHour(args[0]), getHour(args[0]));
        }
        if (paramsInfo.count == 2) {
            builder.withHourMinMax(getHour(args[0]), getHour(args[1]));
            if (getHour(args[0]) != getHour(args[1])) {
                builder.withMinuteMinMax(0, 0);
            }
        }
        if (paramsInfo.count == 4) {
            builder.withHourMinMax(getHour(args[0]), getHour(args[2]))
                    .withMinuteMinMax(getMinute(args[1]), getMinute(args[3]))
                    .withSecondMinMax(0, 0);
        }
        if (paramsInfo.count == 6) {
            builder.withHourMinMax(getHour(args[0]), getHour(args[3]))
                    .withMinuteMinMax(getMinute(args[1]), getMinute(args[4]))
                    .withSecondMinMax(getSecond(args[2]), getSecond(args[5]));
        }
        TimeRange timeRange = builder.build();

        Calendar calendar = getCalendar(now, paramsInfo.useGMT);
        return timeRange.isInRange(calendar);
    }


    /**
     * Evaluates if now is within a date range. Method arguments are as described
     * for {@link PacHelperMethodsNetscape#dateRange(Object...)  }
     *
     * @param now
     * @param args arguments
     * @return true if within range
     * @throws PacDateTimeInputException if arguments were invalid
     */
    public static boolean isInDateRange(Date now, Object... args) throws PacDateTimeInputException {
        ParamsInfo paramsInfo = getParamsInfo(args);

        if (!(paramsInfo.count >= 1 && paramsInfo.count <= 6) || paramsInfo.count == 5 || paramsInfo.count == 3) {
            throw new PacDateTimeInputException("invalid number of arguments");
        }

        DateRange.DateRangeBuilder builder = DateRange.builder();
        if (paramsInfo.count == 1) {
            if (isYear(args[0])) {
                int year = getYear(args[0]);
                builder.withYear(year, year);
            } else if (isMonth(args[0])) {
                int month = getMonth(args[0].toString());
                builder.withMonth(month, month);
            } else if (isDate(args[0])) {
                int date = getDate(args[0]);
                builder.withDate(date, date);
            } else {
                throw new PacDateTimeInputException("invalid argument : " + args[0].toString());
            }
        } else if (paramsInfo.count == 2) {
            if (isYear(args[0])) {
                builder.withYear(getYear(args[0]), getYear(args[1]));
            } else if (isMonth(args[0])) {
                builder.withMonth(getMonth(args[0].toString()), getMonth(args[1].toString()));
            } else if (isDate(args[0])) {
                builder.withDate(getDate(args[0]), getDate(args[1]));
            } else {
                throw new PacDateTimeInputException("invalid argument : " + args[0].toString());
            }
        } else if (paramsInfo.count == 4) {
            if (isMonth(args[0])) {
                builder.withYear(getYear(args[1]), getYear(args[3]))
                        .withMonth(getMonth(args[0].toString()), getMonth(args[2].toString()));
            } else if (isDate(args[0])) {
                builder.withMonth(getMonth(args[1].toString()), getMonth(args[3].toString()))
                        .withDate(getDate(args[0]), getDate(args[2]));
            } else {
                throw new PacDateTimeInputException("invalid argument : " + args[0].toString());
            }
        } else if (paramsInfo.count == 6) {
            builder.withYear(getYear(args[2]), getYear(args[5]))
                    .withMonth(getMonth(args[1].toString()), getMonth(args[4].toString()))
                    .withDate(getDate(args[0]), getDate(args[3]));
        }

        Calendar cal = getCalendar(now, paramsInfo.useGMT);
        return builder.build().isInRange(cal);
    }


    private static boolean isMonth(Object obj) {
        return (obj instanceof CharSequence);
    }

    private static boolean isYear(Object obj) {
        try {
            int val = getInteger(obj);
            return (val >= 1000);
        } catch (PacDateTimeInputException ex) {
            return false;
        }
    }

    private static boolean isDate(Object obj) {
        try {
            int val = getInteger(obj);
            return (val >= 1 && val <= 31);
        } catch (PacDateTimeInputException ex) {
            return false;
        }
    }

    private static int getDate(Object obj) throws PacDateTimeInputException {
        if (!isDate(obj)) {
            throw new PacDateTimeInputException("value " + obj.toString() + " is not a valid day of month");
        }
        return getInteger(obj);
    }

    private static int getYear(Object obj) throws PacDateTimeInputException {
        if (!isYear(obj)) {
            throw new PacDateTimeInputException("value " + obj.toString() + " is not a valid year");
        }
        return getInteger(obj);
    }

    private static int getWeekday(String wd) throws PacDateTimeInputException {
        int indexOf = WEEKDAY_NAMES.indexOf(wd);
        if (indexOf == -1) {
            throw new PacDateTimeInputException("Unknown weekday name : \"" + wd + "\"");
        }
        return indexOf + 1;  // In Calendar, the first weekday (Sunday) is 1
    }

    private static int getMonth(String month) throws PacDateTimeInputException {
        int indexOf = MONTH_NAMES.indexOf(month);
        if (indexOf == -1) {
            throw new PacDateTimeInputException("Unknown month name : \"" + month + "\"");
        }
        return indexOf;  // In Calendar, January is 0
    }


    private static int getInteger(Object obj) throws PacDateTimeInputException {
        if (obj instanceof Integer || obj instanceof Long) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException ex) {
            }
        }
        throw new PacDateTimeInputException("value is " + obj + " is not an integer");
    }

    private static int getHour(Object obj) throws PacDateTimeInputException {
        int hour = getInteger(obj);
        if (!(hour >= 0 && hour <= 23)) {
            throw new PacDateTimeInputException("value is " + hour + " is not a valid hour of day (0-23)");
        }
        return hour;
    }

    private static int getMinute(Object obj) throws PacDateTimeInputException {
        int min = getInteger(obj);
        if (!(min >= 0 && min <= 59)) {
            throw new PacDateTimeInputException("value is " + min + " is not a valid minute (0-59)");
        }
        return min;
    }

    private static int getSecond(Object obj) throws PacDateTimeInputException {
        int sec = getInteger(obj);
        if (!(sec >= 0 && sec <= 59)) {
            throw new PacDateTimeInputException("value is " + sec + " is not a valid second (0-59)");
        }
        return sec;
    }

    private static Calendar getCalendar(Date now, boolean useGMT) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        if (useGMT) {
            cal.setTimeZone(UTC_TIME);
        }
        return cal;
    }


    /**
     * Gets the number of actual arguments passed to a JavaScript
     * function. This is done by counting the number of arguments of type
     * {@code Number} or {@code CharSequence}.
     *
     * <p>
     * This is a convenience method useful when implementing
     * {@link PacHelperMethodsNetscape#dateRange(Object...) dateRange()}
     * ,
     * {@link PacHelperMethodsNetscape#timeRange(Object...) timeRange()}
     * or
     * {@link PacHelperMethodsNetscape#weekdayRange(Object...) weekdayRange()}
     *
     * <p>
     * Note: In some engines, JavaScript function arguments that are not used in the
     * call will have a type of {@code Undefined}.
     *
     * @param objs
     * @return
     */
    private static int getNoOfParams(Object... objs) {
        return (int) Arrays.stream(objs).filter((obj) -> obj instanceof Number || obj instanceof CharSequence).count();
    }

    private static ParamsInfo getParamsInfo(Object... args) {
        int noOfParams = getNoOfParams(args);
        boolean useGMT;
        if (args[noOfParams - 1] instanceof CharSequence) {
            String p = args[noOfParams - 1].toString();
            useGMT = p.equals("GMT");
        } else {
            useGMT = false;
        }
        if (useGMT) {
            noOfParams--;
        }
        return new ParamsInfo(noOfParams, useGMT);
    }


    /**
     * Validation errors on input to {@code weekdayRange()},
     * {@code timeRange()} and {@code dateRange()}.
     */
    public static class PacDateTimeInputException extends Exception {
        public PacDateTimeInputException(String msg) {
            super(msg);
        }
    }

    private static class ParamsInfo {
        private final int count;
        private final boolean useGMT;

        ParamsInfo(int count, boolean useGMT) {
            this.count = count;
            this.useGMT = useGMT;
        }
    }
}

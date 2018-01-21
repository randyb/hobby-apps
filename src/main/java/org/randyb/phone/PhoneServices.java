package org.randyb.phone;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.calendar.HolidayCalendar;
import org.quartz.impl.calendar.WeeklyCalendar;
import org.quartz.impl.matchers.GroupMatcher;
import org.randyb.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.quartz.DateBuilder.dateOf;
import static spark.Spark.get;
import static spark.Spark.post;

public class PhoneServices {
    private static final Logger LOG = LoggerFactory.getLogger(PhoneServices.class);
    private static final String TROPO_KEY = System.getenv("TROPO_KEY");
    private static final String TROPO_URL = "https://api.tropo.com/1.0/sessions?action=create&token=" + TROPO_KEY;

    private static final String GROUP = "org.randyb.scrum";

    private static final Map<String, Integer> JS_DAY_TO_INT = ImmutableMap.of(
        "m", 2, "tu", 3, "w", 4, "th", 5, "f", 6);

    private static final Map<String, Boolean> ALL_WEEKDAYS = ImmutableMap.of(
        "m", true, "tu", true, "w", true, "th", true, "f", true);

    private static final Scheduler SCHEDULER;
    static {
        try {
            URI dbUri = new URI(System.getenv("DATABASE_URL"));
            String POSTGRES_USERNAME = dbUri.getUserInfo().split(":")[0];
            String POSTGRES_PASSWORD = dbUri.getUserInfo().split(":")[1];
            String POSTGRES_URL = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?sslmode=require";

            Properties props = new Properties();
            props.setProperty("org.quartz.scheduler.instanceName", "RandyScheduler2");
            props.setProperty("org.quartz.threadPool.threadCount", "10");
            props.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
            props.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
            props.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
            props.setProperty("org.quartz.jobStore.dataSource", "dataSource");
            props.setProperty("org.quartz.dataSource.dataSource.driver", "org.postgresql.Driver");
            props.setProperty("org.quartz.dataSource.dataSource.URL", POSTGRES_URL);
            props.setProperty("org.quartz.dataSource.dataSource.user", POSTGRES_USERNAME);
            props.setProperty("org.quartz.dataSource.dataSource.password", POSTGRES_PASSWORD);
            props.setProperty("org.quartz.dataSource.dataSource.maxConnections", "10");

            StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
            schedulerFactory.initialize(props);
            SCHEDULER = schedulerFactory.getScheduler();

            // skip company holidays
            WeeklyCalendar weeklyCalendar = new WeeklyCalendar();
            HolidayCalendar calendar = new HolidayCalendar(weeklyCalendar);
            calendar.addExcludedDate(getDate( 4,  7, 2016)); // July 4 2016
            calendar.addExcludedDate(getDate( 5,  9, 2016)); // Sept 5 2016
            calendar.addExcludedDate(getDate(24, 11, 2016)); // Nov 24 2016
            calendar.addExcludedDate(getDate(25, 11, 2016)); // Nov 25 2016
            calendar.addExcludedDate(getDate(23, 12, 2016)); // Dec 23 2016
            calendar.addExcludedDate(getDate(26, 12, 2016)); // Dec 26 2016
            calendar.addExcludedDate(getDate(27, 12, 2016)); // Dec 27 2016
            calendar.addExcludedDate(getDate(28, 12, 2016)); // Dec 28 2016
            calendar.addExcludedDate(getDate(29, 12, 2016)); // Dec 29 2016
            calendar.addExcludedDate(getDate(30, 12, 2016)); // Dec 30 2016

            calendar.addExcludedDate(getDate( 2,  1, 2017)); // Jan  2 2017
            calendar.addExcludedDate(getDate(29,  5, 2017)); // May 29 2017
            calendar.addExcludedDate(getDate( 3,  7, 2017)); // July 3 2017
            calendar.addExcludedDate(getDate( 4,  7, 2017)); // July 4 2017
            calendar.addExcludedDate(getDate( 4,  9, 2017)); // Sept 4 2017
            calendar.addExcludedDate(getDate(23, 11, 2017)); // Nov 23 2017
            calendar.addExcludedDate(getDate(24, 11, 2017)); // Nov 24 2017
            calendar.addExcludedDate(getDate(25, 12, 2017)); // Dec 25 2017
            calendar.addExcludedDate(getDate(26, 12, 2017)); // Dec 26 2017
            calendar.addExcludedDate(getDate(27, 12, 2017)); // Dec 27 2017
            calendar.addExcludedDate(getDate(28, 12, 2017)); // Dec 28 2017
            calendar.addExcludedDate(getDate(29, 12, 2017)); // Dec 29 2017

            SCHEDULER.addCalendar("calendar", calendar, true, false);

            SCHEDULER.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        SCHEDULER.shutdown();
                    } catch (SchedulerException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addServices() {
        post("/phone", (request, response) -> {
            String blob = request.queryParams("numbers");
            List<String> numbers = Splitter
                .on(CharMatcher.WHITESPACE)
                .omitEmptyStrings()
                .trimResults()
                .splitToList(blob);
            makePhoneCalls(numbers);
            return "Success";
        });

        Route getRoute = (request, response) -> {
            Set<TriggerKey> triggerKeys = SCHEDULER.getTriggerKeys(
                GroupMatcher.triggerGroupEquals(GROUP));
            List<PhoneEntry> list = new ArrayList<>();

            for (TriggerKey triggerKey : triggerKeys) {
                Trigger trigger = SCHEDULER.getTrigger(triggerKey);
                JobDetail job = SCHEDULER.getJobDetail(trigger.getJobKey());
                boolean enabled = job.getJobDataMap().getBoolean("enabled");
                String number = job.getJobDataMap().getString("number");
                String name = job.getKey().getName();
                long time = trigger.getNextFireTime().getTime();

                final Map<String, Boolean> days;
                if (trigger instanceof DailyTimeIntervalTrigger) {
                    Set<Integer> daysOfWeek = ((DailyTimeIntervalTrigger) trigger).getDaysOfWeek();
                    days = JS_DAY_TO_INT.entrySet().stream()
                        .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> daysOfWeek.contains(e.getValue())
                        ));
                } else {
                    days = ALL_WEEKDAYS;
                }

                list.add(new PhoneEntry(enabled, name, time, number, days));
            }

            list.sort(Comparator.comparingLong(a -> a.time));

            response.type("application/json");
            return Main.GSON.toJson(list);
        };

        get("/phone/list", getRoute);

        post("/phone/save", (request, response) -> {
            PhoneEntry entry = Main.GSON.fromJson(request.body(), PhoneEntry.class);
            schedulePhoneCall(entry);
            return getRoute.handle(request, response);
        });

        post("/phone/delete", (request, response) -> {
            String name = request.queryParams("name");
            SCHEDULER.deleteJob(JobKey.jobKey(name, GROUP));
            LOG.info("Deleted {}", name);
            return getRoute.handle(request, response);
        });
    }

    private static void schedulePhoneCall(PhoneEntry entry) throws SchedulerException {

        Date date = new Date(entry.time);
        Date dummyDate = new Date(entry.time + 1000);

        Set<Integer> days = entry.days.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .map(JS_DAY_TO_INT::get)
            .collect(Collectors.toSet());

        DailyTimeIntervalScheduleBuilder scheduleBuilder = DailyTimeIntervalScheduleBuilder
            .dailyTimeIntervalSchedule()
            .startingDailyAt(TimeOfDay.hourAndMinuteAndSecondFromDate(date))
            .endingDailyAt(TimeOfDay.hourAndMinuteAndSecondFromDate(dummyDate))
            .withIntervalInHours(1)
            .onDaysOfTheWeek(days);

        DailyTimeIntervalTrigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(entry.name, GROUP)
            .withSchedule(scheduleBuilder)
            .modifiedByCalendar("calendar")
            .startNow()
            .build();

        JobDetail job = JobBuilder.newJob(PhoneJob.class)
            .withIdentity(entry.name, GROUP)
            .usingJobData("enabled", entry.enabled)
            .usingJobData("number", entry.number)
            .build();

        SCHEDULER.scheduleJob(job, ImmutableSet.of(trigger), true);
    }

    private static void makePhoneCalls(List<String> numbers) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(numbers.size());
        List<Future<?>> futures = numbers.stream()
            .map(num -> executor.submit(() -> makeCall(num)))
            .collect(Collectors.toList());
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
    }

    private static void makeCall(String number) {
        try {
            URL url = new URL(TROPO_URL + "&number=" + CharMatcher.DIGIT.retainFrom(number));
            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();
            ByteStreams.copy(inputStream, System.out);
            System.out.println();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Date getDate(int day, int month, int year) {
        return dateOf(0, 0, 0, day, month, year);
    }

    public static class PhoneJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap data = context.getJobDetail().getJobDataMap();
            Date nextFireTime = context.getNextFireTime();

            if (data.getBoolean("enabled")) {
                String number = data.getString("number");
                String name = context.getJobDetail().getKey().getName();
                String now = new Date().toString();
                LOG.info("Calling {} ({}) at {}. Next fire time is {}", name, number, now, nextFireTime);
                //makeCall(number);
            }
        }
    }
}

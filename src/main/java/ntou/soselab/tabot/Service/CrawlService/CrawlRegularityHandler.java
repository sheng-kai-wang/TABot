package ntou.soselab.tabot.Service.CrawlService;

//import org.quartz.CronTrigger;
//import org.quartz.JobDetail;
//import org.quartz.Scheduler;
//import org.quartz.SchedulerException;
//import org.quartz.impl.StdSchedulerFactory;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import static org.quartz.CronScheduleBuilder.cronSchedule;
//import static org.quartz.JobBuilder.newJob;
//import static org.quartz.TriggerBuilder.newTrigger;

public class CrawlRegularityHandler {
//    public void run() {
//        try {
//            // 建立一個scheduler
//            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
//
//            //真正執行的任務並不是Job介面的實例，而是用反射的方式實例化的一個JobDetail實例
//            JobDetail job = newJob(GradesCrawler.class).withIdentity("job1", "group1").build();
//
//            // 定義一個觸發器，group1將每隔一小時執行一次
//            CronTrigger trigger = newTrigger()
//                    .withIdentity("trigger1", "group1")
//                    .withSchedule(cronSchedule("? ? * ? ? ? ?"))
//                    .build();
//
//            // 排入scheduler裡，回傳執行時間
//            Date performTime = scheduler.scheduleJob(job, trigger);
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
//            System.out.println(job.getKey() + " 已被安排執行於: " + simpleDateFormat.format(performTime) + "，" +
//                    "並且以如下重複規則重複執行: " + trigger.getCronExpression());
//
//            scheduler.start();
//
//        } catch (SchedulerException e) {
//            throw new RuntimeException(e);
//        }
//    }
}

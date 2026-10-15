package za.co.rmb.approvalforum.web.quartz;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;

public class SchedulerProvider implements Provider<Scheduler> {

    private Scheduler scheduler;

    @Inject
    public SchedulerProvider(SchedulerJobFactory jobFactory) throws SchedulerException {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.threadPool.threadCount", "5");
        scheduler = new StdSchedulerFactory(properties).getScheduler();
        scheduler.setJobFactory(jobFactory);
    }

    @Override
    public Scheduler get() {
        return scheduler;
    }

}


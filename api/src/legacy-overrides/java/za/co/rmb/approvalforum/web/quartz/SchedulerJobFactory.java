package za.co.rmb.approvalforum.web.quartz;

import jakarta.inject.Singleton;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring version of the legacy job factory, which returned {@code injector.getInstance(jobClass)}.
 *
 * <p>Guice created each job with its {@code @Inject} constructor and fields, and returned the same instance
 * every time for a job class annotated {@code @Singleton} (all the legacy jobs are). This does the same with
 * Spring's bean factory.</p>
 */
public class SchedulerJobFactory implements JobFactory {

    private final AutowireCapableBeanFactory beanFactory;
    private final Map<Class<?>, Job> singletons = new ConcurrentHashMap<>();

    public SchedulerJobFactory(AutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();
        if (jobClass.isAnnotationPresent(Singleton.class)) {
            return singletons.computeIfAbsent(jobClass, type -> (Job) beanFactory.createBean(type));
        }
        return beanFactory.createBean(jobClass);
    }
}

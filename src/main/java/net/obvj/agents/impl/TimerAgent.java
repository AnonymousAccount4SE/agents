package net.obvj.agents.impl;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.obvj.agents.AgentType;
import net.obvj.agents.conf.AgentConfiguration;
import net.obvj.agents.util.DateUtils;
import net.obvj.agents.util.TimeInterval;

/**
 * A thread-safe extensible Agent for tasks that are scheduled in the system to run
 * repeatedly, given an interval that is particular to each task. Available operations
 * are: 'start', 'stop', 'run' and 'reset'
 *
 * @author oswaldo.bapvic.jr
 */
public abstract class TimerAgent extends AbstractAgent
{
    private static final Logger LOG = LoggerFactory.getLogger(TimerAgent.class);

    private TimeInterval interval;

    private AgentThreadFactory threadFactory;
    private ScheduledExecutorService schedule;

    /**
     * Builds a {@link TimerAgent} from the given configuration.
     *
     * @param configuration the {@link AgentConfiguration} to be set
     */
    TimerAgent(AgentConfiguration configuration)
    {
        super(configuration);

        if (configuration.getType() != AgentType.TIMER)
        {
            throw new IllegalArgumentException("Not a timer agent");
        }

        TimeInterval timeInterval = TimeInterval.of(configuration.getFrequency());
        this.interval = timeInterval;

        threadFactory = new AgentThreadFactory(getName());
        schedule = Executors.newSingleThreadScheduledExecutor(threadFactory);

        setState(State.SET);
    }

    /**
     * Starts this agent timer considering the interval settled in this object for execution.
     */
    @Override
    public final void onStart()
    {
        LOG.info("Starting agent: {}", getName());
        LOG.info("Agent {} scheduled to run every {}.", getName(), interval);

        Date start = DateUtils.getExactStartDateEvery(interval.getDuration(), interval.getTimeUnit());
        schedule.scheduleAtFixedRate(this, (start.getTime() - System.currentTimeMillis()), interval.toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS);

        if (LOG.isDebugEnabled())
        {
            LOG.debug("First execution of {} will be at: {}", getName(), DateUtils.formatDate(start));
        }
    }

    /**
     * Terminates this agent timer gracefully. Does not interfere with a currently executing
     * task, if it exists.
     */
    @Override
    public final void onStop()
    {
        schedule.shutdown();
    }

    @Override
    public void afterRun()
    {
        // Nothing required after task
    }

    /**
     * @return A string with current agent status in JSON format
     */
    @Override
    public String getStatusString()
    {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
        builder.append("name", getName()).append("type", getType()).append("status", getState())
                .append("startDate", (DateUtils.formatDate(startDate)))
                .append("lastExecutionStartDate", (DateUtils.formatDate(lastExecutionDate)))
                .append("lastExecutionDuration", formatLastExecutionDuration())
                .append("averageExecutionDuration", formatAverageExecutionDuration())
                .append("frequency", interval);
        return builder.build();
    }

}

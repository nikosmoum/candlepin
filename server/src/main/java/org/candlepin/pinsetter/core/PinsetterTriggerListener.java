/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.pinsetter.core;

import com.google.inject.Inject;
import org.candlepin.controller.mode.CandlepinModeManager;
import org.candlepin.controller.mode.CandlepinModeManager.Mode;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;

/**
 * This component receives events around job status and performs actions to
 * allow for the job in question to run outside of a request scope, as well as
 * record the status of the job for later retrieval.
 */
public class PinsetterTriggerListener extends TriggerListenerSupport {
    private static Logger log = LoggerFactory.getLogger(PinsetterTriggerListener.class);
    private CandlepinModeManager modeManager;
    private JobCurator jobCurator;

    @Inject
    public PinsetterTriggerListener(CandlepinModeManager modeManager, JobCurator jobCurator) {
        this.modeManager = modeManager;
        this.jobCurator =  jobCurator;
    }

    @Override
    public String getName() {
        return "Suspend mode trigger listener";
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        if (this.modeManager.getCurrentMode() == Mode.SUSPEND) {
            log.debug("Pinsetter trigger listener detected SUSPEND mode; vetoing job execution");
            return true;
        }

        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        log.warn("Trigger misfired for for Job: \nKey: {}\nJob Key: {}\nStart: {}\nEnd: {}\n" +
            "Final Fire Time: {}\nNext Fire Time: {}\nMisfire Instruction: {}\nPriority: {}",
            trigger.getKey(),
            trigger.getJobKey(),
            trigger.getStartTime(),
            trigger.getEndTime(),
            trigger.getFinalFireTime(),
            trigger.getNextFireTime(),
            trigger.getMisfireInstruction(),
            trigger.getPriority());

        try {
            String id = trigger.getJobKey().getName();
            JobStatus js = jobCurator.get(id);
            if (js == null) {
                throw new RuntimeException("No JobStatus for job with id of " + id);
            }

            // if may not refire again, change to fail
            if (!trigger.mayFireAgain()) {
                log.warn("Job {} not allowed to fire again. Setting state to FAILED.", id);
                js.setState(JobStatus.JobState.FAILED);
                js.setResult("Failed run. Will not attempt again.");
            }
            else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                js.setResult("Will reattempt job at or after " + sdf.format(trigger.getNextFireTime()));
                js.setState(JobStatus.JobState.PENDING);
            }
            jobCurator.merge(js);
        }
        catch (Exception e) {
            log.error("Unable to capture misfire into job status: {}", e.getMessage());
        }
    }
}

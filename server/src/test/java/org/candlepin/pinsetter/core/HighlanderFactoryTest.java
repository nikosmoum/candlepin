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

import static org.junit.jupiter.api.Assertions.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;

import org.candlepin.test.DatabaseTestFixture;

import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.JobFactory;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.text.ParseException;

import javax.inject.Inject;

/**
 * HighlanderFactoryTest
 */
public class HighlanderFactoryTest extends DatabaseTestFixture {
    @Inject private JobFactory hf;

    @Test
    public void testNewJob() throws SchedulerException, ParseException {
        assertNotNull(hf);
        try {
            hf.newJob(null, null);
            fail("should've died with npe");
        }
        catch (NullPointerException npe) {
            // Expected
        }

        String crontab = "0 0 12 * * ?";
        JobDetail jd = newJob(TestJob.class)
            .withIdentity("testjob", "group")
            .build();

        Trigger trigger = newTrigger()
            .withIdentity("testjob", "group")
            .withSchedule(cronSchedule(crontab))
            .build();

        TriggerFiredBundle tfb = new TriggerFiredBundle(jd, (OperableTrigger) trigger, null,
            false, null, null, null, null);
        Job j = hf.newJob(tfb, null);
        assertNotNull(j);
    }
}

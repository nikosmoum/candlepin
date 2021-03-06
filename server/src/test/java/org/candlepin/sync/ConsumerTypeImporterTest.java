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
package org.candlepin.sync;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.candlepin.common.config.Configuration;
import org.candlepin.common.config.MapConfiguration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.model.ConsumerType;
import org.candlepin.model.ConsumerTypeCurator;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;

/**
 * ConsumerTypeImporterTest
 */
public class ConsumerTypeImporterTest {
    private Configuration config;

    @Before
    public void init() {
        config = new MapConfiguration(new HashMap<String, String>() {

            {
                put(ConfigProperties.FAIL_ON_UNKNOWN_IMPORT_PROPERTIES, "false");
            }
        });
    }

    @Test
    public void testDeserialize() throws IOException {
        String consumerTypeString = "{\"id\":15, \"label\":\"prosumer\"}";

        Reader reader = new StringReader(consumerTypeString);

        ConsumerType consumerType = new ConsumerTypeImporter(null).createObject(
            TestSyncUtils.getTestSyncUtils(config), reader);

        assertEquals("prosumer", consumerType.getLabel());
    }

    @Test
    public void testDeserializeIdIsNull() throws IOException {
        String consumerTypeString = "{\"id\":15, \"label\":\"prosumer\"}";

        Reader reader = new StringReader(consumerTypeString);

        ConsumerType consumerType = new ConsumerTypeImporter(null).createObject(
            TestSyncUtils.getTestSyncUtils(config), reader);

        assertEquals(null, consumerType.getId());
    }

    @Test
    public void testSingleConsumerTypeInDbAndListCausesNoChange() {
        final ConsumerType testType = new ConsumerType();
        testType.setLabel("prosumer");

        ConsumerTypeCurator curator = mock(ConsumerTypeCurator.class);

        when(curator.getByLabel("prosumer")).thenReturn(testType);

        ConsumerTypeImporter importer = new ConsumerTypeImporter(curator);
        importer.store(new HashSet<ConsumerType>() {
            {
                add(testType);
            }
        });

        verify(curator, never()).create(testType);
        verify(curator, never()).merge(testType);
    }

    @Test
    public void testSingleConsumerTypeInListEmptyDbCausesInsert() {
        final ConsumerType testType = new ConsumerType();
        testType.setLabel("prosumer");

        ConsumerTypeCurator curator = mock(ConsumerTypeCurator.class);

        when(curator.getByLabel("prosumer")).thenReturn(null);

        ConsumerTypeImporter importer = new ConsumerTypeImporter(curator);
        importer.store(new HashSet<ConsumerType>() {
            {
                add(testType);
            }
        });

        verify(curator).create(testType);
        verify(curator, never()).merge(testType);
    }
}

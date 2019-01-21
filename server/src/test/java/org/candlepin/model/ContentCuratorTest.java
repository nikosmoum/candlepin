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
package org.candlepin.model;

import org.candlepin.test.DatabaseTestFixture;
import org.candlepin.test.TestUtil;

import org.junit.jupiter.api.BeforeEach;

import java.util.HashSet;

import javax.inject.Inject;

/**
 * ContentCuratorTest
 */
public class ContentCuratorTest extends DatabaseTestFixture {
    @Inject private ContentCurator contentCurator;
    @Inject private OwnerCurator ownerCurator;

    private Content updates;
    private Owner owner;

    /* FIXME: Add Arches here */

    @BeforeEach
    public void setUp() {
        this.owner = new Owner("Example-Corporation");
        ownerCurator.create(owner);

        updates = TestUtil.createContent("100", "Test Content 1");

        updates.setLabel("test-content-label-1");
        updates.setType("yum-1");
        updates.setVendor("test-vendor-1");
        updates.setContentUrl("test-content-url-1");
        updates.setGpgUrl("test-gpg-url-1");
        updates.setArches("test-arch1,test-arch2");

        updates.setRequiredTags("required-tags");
        updates.setReleaseVersion("releaseVer");
        updates.setMetadataExpiration(new Long(1));
        updates.setModifiedProductIds(new HashSet<String>() { { add("productIdOne"); } });
    }
}

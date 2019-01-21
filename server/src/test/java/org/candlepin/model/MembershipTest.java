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

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

public class MembershipTest extends DatabaseTestFixture {
    @Inject private OwnerCurator ownerCurator;
    @Inject private UserCurator userCurator;

    @Test
    public void testCreate() throws Exception {
        Owner o = new Owner("testowner");
        ownerCurator.create(o);
        Set<Owner> owners = new HashSet<>();
        owners.add(o);

        String username = "TESTUSER";
        String password = "sekretpassword";
        User user = new User(username, password);
        userCurator.create(user);

        // TODO:  Finish up here...
    }

}

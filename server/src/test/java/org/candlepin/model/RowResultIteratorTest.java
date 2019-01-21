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

import static org.junit.jupiter.api.Assertions.*;

import org.candlepin.test.DatabaseTestFixture;

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;



public class RowResultIteratorTest extends DatabaseTestFixture {

    private Session session;

    @BeforeEach
    public void setup() {
        this.session = (Session) this.getEntityManager().getDelegate();
    }

    @Test
    public void testHasNextWithElements() {
        this.createOwner();
        Query query = this.session.createQuery("SELECT o FROM Owner o");

        RowResultIterator iterator = new RowResultIterator(query.scroll(ScrollMode.FORWARD_ONLY));

        try {
            assertTrue(iterator.hasNext());
        }
        finally {
            iterator.close();
        }
    }

    @Test
    public void testHasNextWithoutElements() {
        Query query = this.session.createQuery("SELECT o FROM Owner o");

        RowResultIterator iterator = new RowResultIterator(query.scroll(ScrollMode.FORWARD_ONLY));

        try {
            assertFalse(iterator.hasNext());
        }
        finally {
            iterator.close();
        }
    }

    @Test
    public void testNextWithElements() {
        Owner owner1 = this.createOwner();
        Owner owner2 = this.createOwner();
        Owner owner3 = this.createOwner();

        Query query = this.session.createQuery("SELECT o FROM Owner o");

        RowResultIterator iterator = new RowResultIterator(query.scroll(ScrollMode.FORWARD_ONLY));

        try {
            List<Owner> owners = new LinkedList<>();

            // Note: Since we're testing everything in isolation here, we can't
            // be expecting .hasNext to be functional here. :)
            owners.add((Owner) iterator.next()[0]);
            owners.add((Owner) iterator.next()[0]);
            owners.add((Owner) iterator.next()[0]);

            assertTrue(owners.contains(owner1));
            assertTrue(owners.contains(owner2));
            assertTrue(owners.contains(owner3));
        }
        finally {
            iterator.close();
        }
    }

    @Test
    public void testNextWithoutElements() {
        Query query = this.session.createQuery("SELECT o FROM Owner o");

        RowResultIterator iterator = new RowResultIterator(query.scroll(ScrollMode.FORWARD_ONLY));

        assertThrows(NoSuchElementException.class, () -> iterator.next()); // Kaboom
        iterator.close();
    }

    @Test
    public void testRemoveAlwaysFails() {
        this.createOwner();
        Query query = this.session.createQuery("SELECT o FROM Owner o");

        RowResultIterator iterator = new RowResultIterator(query.scroll(ScrollMode.FORWARD_ONLY));

        iterator.next();
        assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
        iterator.close();
    }

}

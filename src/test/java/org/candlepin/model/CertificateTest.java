/*
 * Copyright (c) 2009 - 2023 Red Hat, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.candlepin.test.DatabaseTestFixture;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;



public class CertificateTest extends DatabaseTestFixture {

    protected SubscriptionsCertificate createSubCert(String key, String cert) {
        return createSubCert(key, cert, new Date());
    }

    protected SubscriptionsCertificate createSubCert(String key, String cert, Date dt) {
        SubscriptionsCertificate sc = new SubscriptionsCertificate();
        CertificateSerial ser = new CertificateSerial(dt);
        certSerialCurator.create(ser);
        sc.setCert(cert);
        sc.setKey(key);
        sc.setSerial(ser);
        return sc;
    }

    @Test
    public void testList() throws Exception {
        List<SubscriptionsCertificate> certificates = subscriptionsCertificateCurator.listAll();
        int beforeCount = certificates.size();

        for (int i = 0; i < 10; i++) {
            subscriptionsCertificateCurator.create(createSubCert("key" + i, "cert" + i));
        }

        certificates = subscriptionsCertificateCurator.listAll();
        int afterCount = certificates.size();
        assertEquals(10, afterCount - beforeCount);
    }

    @Test
    public void testLookup() throws Exception {

        SubscriptionsCertificate certificate = createSubCert("key", "cert");
        subscriptionsCertificateCurator.create(certificate);
        SubscriptionsCertificate lookedUp = subscriptionsCertificateCurator.get(certificate.getId());

        assertNotNull(lookedUp);
        assertEquals(certificate.getId(), lookedUp.getId());
        assertEquals(certificate.getKey(), lookedUp.getKey());
        assertEquals(certificate.getCert(), lookedUp.getCert());
        assertEquals(certificate.getSerial(), lookedUp.getSerial());
    }

    @Test
    public void createDuplicateCertSameOwnerThrowsException() throws Exception {
        Date now = new Date();
        SubscriptionsCertificate certificate1 = createSubCert("not a cert", "booya", now);
        SubscriptionsCertificate certificate2 = createSubCert("not a cert", "booya", now);

        subscriptionsCertificateCurator.create(certificate1);
        subscriptionsCertificateCurator.create(certificate2);
    }
}

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
package org.candlepin.testext.hostedtest;

import org.candlepin.service.CloudRegistrationAdapter;
import org.candlepin.service.ProductServiceAdapter;
import org.candlepin.service.SubscriptionServiceAdapter;

import com.google.inject.AbstractModule;



/**
 * Custom Guice module to inject the hosted-test resources and adapters for simulating upstream
 * services and adapters for hosted-mode spec tests.
 */
public class HostedTestModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(HostedTestResource.class);
        bind(HostedTestDataStore.class).asEagerSingleton();

        bind(SubscriptionServiceAdapter.class).to(HostedTestSubscriptionServiceAdapter.class)
            .asEagerSingleton();
        bind(ProductServiceAdapter.class).to(HostedTestProductServiceAdapter.class)
            .asEagerSingleton();
        bind(CloudRegistrationAdapter.class).to(HostedTestCloudRegistrationAdapter.class)
            .asEagerSingleton();
    }

}

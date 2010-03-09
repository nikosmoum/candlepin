/**
 * Copyright (c) 2009 Red Hat, Inc.
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
package org.fedoraproject.candlepin.resource;

import com.sun.jersey.api.client.ClientResponse.Status;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Thrown when a resource is not found.
 */
public class ForbiddenException extends WebApplicationException {
    /**
     * 
     */
    private static final long serialVersionUID = 8452977854606374666L;

    /**
     * ctor
     * @param message Exception message string.
     */
    public ForbiddenException(String message) {
        super(Response.status(Status.FORBIDDEN)
                .entity(message)
                .type("text/plain")
                .build()
        );
    }
}

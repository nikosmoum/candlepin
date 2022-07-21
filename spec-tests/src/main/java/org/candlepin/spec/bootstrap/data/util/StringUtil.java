/**
 * Copyright (c) 2009 - 2022 Red Hat, Inc.
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

package org.candlepin.spec.bootstrap.data.util;

import java.util.Random;

public final class StringUtil {

    private static final Random RANDOM = new Random();

    private StringUtil() {
        throw new UnsupportedOperationException();
    }

    public static String random(String prefix) {
        return prefix + "-" + System.nanoTime() + "-" + RANDOM.nextInt(100000);
    }

    /**
     * Generates a random suffix of 7 characters or fewer.
     *
     * @param base
     *  The base string to concatenate to the generated suffix
     *
     * @return
     *  the provided base string concatenated with a randomly generated suffix
     */
    public static String randomSuffix(String base) {
        return base + "-" + RANDOM.nextInt(100000);
    }

}

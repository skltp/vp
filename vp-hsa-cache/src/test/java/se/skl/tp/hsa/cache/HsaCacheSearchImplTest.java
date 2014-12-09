/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.hsa.cache;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.commons.lang.StringUtils;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HsaCacheSearchImplTest {

    private static HsaCache impl = null;

    @BeforeClass
    public static void setup() {
        URL url = HsaCacheSearchImplTest.class.getClassLoader().getResource("simpleTest.xml");
        impl = new HsaCacheImpl().init(url.getFile());
    }

    /**
     * No nodes match "NOT FOUND", "" or null
     */
    @Test
    public void testSearchNoMatch() {
        assertEquals(0, impl.freeTextSearch("NOT FOUND", -1).size());
        assertEquals(0, impl.freeTextSearch("",          -1).size());
        assertEquals(0, impl.freeTextSearch(null,        -1).size());
    }

    /**
     * 1 node match the HSA ID "SE0000000002-1234"
     */
    @Test
    public void testSearchOneHsaId() {

        String hsaId = "SE0000000002-1234";
        List<HsaNodeInfo> list = impl.freeTextSearch(hsaId, -1);

        assertEquals(1, list.size());
        assertEquals(hsaId, list.get(0).getHsaId());
    }

    /**
     * 1 node match "Nässjö VC DLM" in its DN
     */
    @Test
    public void testSearchOneDN() {

        String vc = "Nässjö VC DLM";
        List<HsaNodeInfo> list = impl.freeTextSearch(vc, -1);

        assertEquals(1, list.size());
        assertTrue(StringUtils.containsIgnoreCase(list.get(0).getDn().toString(), vc));
    }

    /**
     * 5 nodes match the HSA ID "SE 1234"
     */
    @Test
    public void testSearchMultipleHsaId() {

        List<HsaNodeInfo> list = impl.freeTextSearch("SE 1234", -1);

        assertEquals(5, list.size());
    }

    /**
     * 2 nodes match "Nässjö VC" in their DN
     */
    @Test
    public void testSearchMultipleDN() {

        List<HsaNodeInfo> list = impl.freeTextSearch("Nässjö VC", -1);

        assertEquals(2, list.size());
    }

    /**
     * 5 nodes match the HSA ID "SE 1234", but we limit the result to max 2 hits
     */
    @Test
    public void testSearchMultipleWithMaxNoOfHits() {

        List<HsaNodeInfo> list = impl.freeTextSearch("SE 1234", 2);

        assertEquals(2, list.size());
    }

    /**
     * 5 nodes match the HSA ID "SE 1234", but only four match "Höglandets sjukvårdsområde" in their DN
     *
     */
    @Test
    public void testSearchBothHsaIdAndDN() {

        List<HsaNodeInfo> list = impl.freeTextSearch("SE 1234 Höglandets sjukvårdsområde", -1);

        assertEquals(4, list.size());
    }
}

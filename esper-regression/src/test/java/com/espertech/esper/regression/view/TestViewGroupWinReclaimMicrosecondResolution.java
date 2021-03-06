/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.util.SupportEngineFactory;
import junit.framework.TestCase;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestViewGroupWinReclaimMicrosecondResolution extends TestCase {
    private Map<TimeUnit, EPServiceProvider> epServices;

    public void setUp() {
        epServices = SupportEngineFactory.setupEnginesByTimeUnit();
    }

    public void tearDown() {
        epServices = null;
    }

    public void testReclaim() {
        for (EPServiceProvider epService : epServices.values()) {
            epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        }

        runAssertionEventTime(epServices.get(TimeUnit.MILLISECONDS), 5000);
        runAssertionEventTime(epServices.get(TimeUnit.MICROSECONDS), 5000000);
    }

    private static void runAssertionEventTime(EPServiceProvider epService, long flipTime) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("isolated");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String epl = "@Hint('reclaim_group_aged=1,reclaim_group_freq=5') select * from SupportBean#groupwin(theString)#keepall";
        EPStatement stmt = isolated.getEPAdministrator().createEPL(epl, "s0", null);

        isolated.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertCount(stmt, 1);

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime - 1));
        isolated.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertCount(stmt, 2);

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime));
        isolated.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertCount(stmt, 2);

        isolated.destroy();
    }

    private static void assertCount(EPStatement stmt, long count) {
        assertEquals(count, EPAssertionUtil.iteratorCount(stmt.iterator()));
    }
}

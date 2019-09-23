/*
 * Copyright (c)  2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.extension.execution.list;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.event.Event;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;
import io.siddhi.core.util.EventPrinter;
import io.siddhi.core.util.SiddhiTestHelper;
import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CloneFunctionExtensionTestCase {
    private static final Logger log = Logger.getLogger(CloneFunctionExtensionTestCase.class);
    private AtomicInteger count = new AtomicInteger(0);
    private volatile boolean eventArrived;

    @BeforeMethod
    public void init() {
        count.set(0);
        eventArrived = false;
    }

    @Test
    public void testCloneFunctionExtension() throws InterruptedException {
        log.info("CloneFunctionExtension TestCase");

        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "\ndefine stream inputStream (symbol string, price long, volume long);";
        String query = ("@info(name = 'query1') from inputStream " +
                "select symbol,price,list:create() as tmpList" +
                " insert into tmpStream;" +
                "@info(name = 'query2') from tmpStream  select symbol,price,tmpList, " +
                "list:add(tmpList,symbol) as list1" +
                " insert into outputStream;" +
                "@info(name = 'query3') " +
                "from outputStream " +
                "select list:clone(list1) as cloneList, list1 " +
                " insert into outputStream2;"
        );

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(
                inStreamDefinition + query);

        siddhiAppRuntime.addCallback("outputStream2", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    count.incrementAndGet();
                    eventArrived = true;
                    AssertJUnit.assertNotSame(event.getData(0), event.getData(1));
                    AssertJUnit.assertEquals(((List) event.getData(0)).get(0), ((List) event.getData(1)).get(0));
                }
            }
        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("inputStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 100, 100L});
        SiddhiTestHelper.waitForEvents(100, 1, count, 60000);

        AssertJUnit.assertTrue(eventArrived);
        AssertJUnit.assertEquals(1, count.get());

        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testCloneFunctionExtension1() {
        log.info("CloneFunctionExtension TestCase with test attributeExpressionExecutors length");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "\ndefine stream inputStream (symbol string, price long, volume long);";
        String query = ("@info(name = 'query1') from inputStream " +
                "select symbol,price,list:create() as tmpList" +
                " insert into tmpStream;" +
                "@info(name = 'query2') from tmpStream  select symbol,price,tmpList, list:add(tmpList,symbol) " +
                "as list1 insert into outputStream;" +
                "@info(name = 'query3') from outputStream " +
                "select list:isEmpty(list1) as isList1,list:clone(symbol,price) as isList2" +
                " insert into outputStream2;"
        );

        siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
    }
}

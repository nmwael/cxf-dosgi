/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.dosgi.itests.multi;

import javax.inject.Inject;

import org.apache.aries.rsa.spi.ExportPolicy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

/**
 * Deploys the sample SOAP service and zookeeper discovery.
 * Then checks the service can be called via plain CXF and is announced in zookeeper
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestExportPolicy extends AbstractDosgiTest {
    
    @Inject
    @Filter("(name=cxf)")
    ExportPolicy policy;
    
    @Configuration
    public static Option[] configure() throws Exception {
        return new Option[] //
        {//
         basicTestOptions(), //
         //debug(),
        };
    }

    @Test
    public void testPolicyPresent() throws Exception {
        Assert.assertNotNull(policy);
    }
    

}

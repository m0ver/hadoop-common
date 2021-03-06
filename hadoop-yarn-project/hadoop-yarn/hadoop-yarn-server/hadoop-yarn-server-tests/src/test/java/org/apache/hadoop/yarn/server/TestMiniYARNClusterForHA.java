/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.hadoop.yarn.server;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetClusterMetricsRequest;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.resourcemanager.AdminService;
import org.apache.hadoop.yarn.server.resourcemanager.ResourceManager;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

public class TestMiniYARNClusterForHA {
  MiniYARNCluster cluster;

  @Before
  public void setup() throws IOException, InterruptedException {
    Configuration conf = new YarnConfiguration();

    cluster = new MiniYARNCluster(TestMiniYARNClusterForHA.class.getName(),
        2, 1, 1, 1);
    cluster.init(conf);
    cluster.start();

    cluster.getResourceManager(0).getRMContext().getRMAdminService()
        .transitionToActive(new HAServiceProtocol.StateChangeRequestInfo(
            HAServiceProtocol.RequestSource.REQUEST_BY_USER));

    assertFalse("RM never turned active", -1 == cluster.getActiveRMIndex());
  }

  @Test
  public void testClusterWorks() throws YarnException, InterruptedException {
    ResourceManager rm = cluster.getResourceManager(0);
    GetClusterMetricsRequest req = GetClusterMetricsRequest.newInstance();

    for (int i = 0; i < 600; i++) {
      if (1 == rm.getClientRMService().getClusterMetrics(req)
          .getClusterMetrics().getNumNodeManagers()) {
        return;
      }
      Thread.sleep(100);
    }
    fail("NodeManager never registered with the RM");
  }
}

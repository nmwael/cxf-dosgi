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
package org.apache.cxf.dosgi.discovery.zookeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.dosgi.discovery.local.util.Utils;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the EndpointListeners and the scopes they are interested in.
 * For each scope with interested EndpointListeners an InterfaceMonitor is created.
 * The InterfaceMonitor calls back when it detects added or removed external Endpoints.
 * These events are then forwarded to all interested EndpointListeners
 */
public class InterfaceMonitorManager {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMonitorManager.class);

    private final ZooKeeper zooKeeper;
    private final Map<ServiceReference, List<String> /* scopes of the epl */> handledEndpointListeners
        = new HashMap<ServiceReference, List<String>>();
    private final Map<String /* scope */, Interest> interestingScopes = new HashMap<String, Interest>();
    private final BundleContext bctx;

    protected static class Interest {
        List<ServiceReference> relatedServiceListeners = new CopyOnWriteArrayList<ServiceReference>();
        InterfaceMonitor im;
    }

    public InterfaceMonitorManager(BundleContext bctx, ZooKeeper zooKeeper) {
        this.bctx = bctx;
        this.zooKeeper = zooKeeper;
    }

    public synchronized void addInterest(ServiceReference sref, String scope, String objClass) {
        Interest interest = interestingScopes.get(scope);
        if (interest == null) {
            interest = new Interest();
            interestingScopes.put(scope, interest);
        }

        if (!interest.relatedServiceListeners.contains(sref)) {
            interest.relatedServiceListeners.add(sref);
        }

        if (interest.im == null) {
            interest.im = createInterfaceMonitor(scope, objClass, interest);
            interest.im.start();
        }

        List<String> handledScopes = handledEndpointListeners.get(sref);
        if (handledScopes == null) {
            handledScopes = new ArrayList<String>(1);
            handledEndpointListeners.put(sref, handledScopes);
        }

        if (!handledScopes.contains(scope)) {
            handledScopes.add(scope);
        }
    }

    /**
     * Only for test case!
     */
    protected synchronized Map<String, Interest> getInterestingScopes() {
        return interestingScopes;
    }

    /**
     * Only for test case!
     */
    protected synchronized Map<ServiceReference, List<String>> getHandledEndpointListeners() {
        return handledEndpointListeners;
    }

    private InterfaceMonitor createInterfaceMonitor(final String scope, String objClass, final Interest interest) {
        // holding this object's lock in the callbacks can lead to a deadlock with InterfaceMonitor
        EndpointListener epListener = new EndpointListener() {

            public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, scope, false, interest.relatedServiceListeners);
            }

            public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
                notifyListeners(endpoint, scope, true, interest.relatedServiceListeners);
            }
        };
        return new InterfaceMonitor(zooKeeper, objClass, epListener, scope);
    }

    public synchronized void removeInterest(ServiceReference sref) {
        List<String> handledScopes = handledEndpointListeners.get(sref);
        if (handledScopes == null) {
            return;
        }

        for (String scope : handledScopes) {
            Interest i = interestingScopes.get(scope);
            if (i != null) {
                i.relatedServiceListeners.remove(sref);
                if (i.relatedServiceListeners.isEmpty()) {
                    i.im.close();
                    interestingScopes.remove(scope);
                }
            }
        }
        handledEndpointListeners.remove(sref);
    }

    private void notifyListeners(EndpointDescription epd, String currentScope, boolean isAdded,
            List<ServiceReference> relatedServiceListeners) {
        for (ServiceReference sref : relatedServiceListeners) {
            Object service = bctx.getService(sref);
            try {
                if (service instanceof EndpointListener) {
                    EndpointListener epl = (EndpointListener) service;
                    LOG.trace("matching {} against {}", epd, currentScope);
                    if (Utils.matchFilter(bctx, currentScope, epd)) {
                        LOG.debug("Matched {} against {}", epd, currentScope);
                        notifyListener(epd, currentScope, isAdded, sref.getBundle(), epl);
                    }
                }
            } finally {
                if (service != null) {
                    bctx.ungetService(sref);
                }
            }
        }
    }

    private void notifyListener(EndpointDescription epd, String currentScope, boolean isAdded,
                                Bundle eplBundle, EndpointListener epl) {
        if (eplBundle == null) {
            LOG.info("listening service was unregistered, ignoring");
        } else if (isAdded) {
            LOG.info("calling EndpointListener.endpointAdded: " + epl + " from bundle "
                    + eplBundle.getSymbolicName() + " for endpoint: " + epd);
            epl.endpointAdded(epd, currentScope);
        } else {
            LOG.info("calling EndpointListener.endpointRemoved: " + epl + " from bundle "
                    + eplBundle.getSymbolicName() + " for endpoint: " + epd);
            epl.endpointRemoved(epd, currentScope);
        }
    }

    public synchronized void close() {
        for (Interest interest : interestingScopes.values()) {
            interest.im.close();
        }
        interestingScopes.clear();
        handledEndpointListeners.clear();
    }
}

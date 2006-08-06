/*****************************************************************************
 * Copyright (C) NanoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/

package org.nanocontainer.nanowar.server;

import groovy.util.NodeBuilder;

import java.util.Map;

import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;

public class ContextBuilder extends NodeBuilder {
        private final PicoContextHandler context;

        public ContextBuilder(PicoContextHandler context) {
            this.context = context;
        }
        protected Object createNode(Object name, Map map) {
            if (name.equals("filter")) {
                FilterHolder filter = context.addFilterWithMapping(
                        (Class) map.remove("class"),
                        (String) map.remove("path"),
                        extractDispatchers(map));
                FilterHolderBuilder builder = new FilterHolderBuilder(filter);
                return builder;
            } else if (name.equals("servlet")) {
                ServletHolder servlet = context.addServletWithMapping(
                        (Class) map.remove("class"),
                        (String) map.remove("path"));
                return new ServletHolderBuilder(servlet);
            }
            return null;
        }
        
        private int extractDispatchers(Map map) {
            Object dispatchers = map.remove("dispatchers");
            if ( dispatchers != null ){
              return ((Integer) dispatchers).intValue();
            }
            // default value
            return 0;
        }

    }
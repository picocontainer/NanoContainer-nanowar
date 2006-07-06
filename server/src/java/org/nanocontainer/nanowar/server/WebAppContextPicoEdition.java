/*****************************************************************************
 * Copyright (C) NanoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/

package org.nanocontainer.nanowar.server;

import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebXmlConfiguration;
import org.mortbay.jetty.webapp.Configuration;
import org.picocontainer.PicoContainer;

/**
 * @deprecated - to be replaced by forthcoming 'Jervlet' release
 */
public class WebAppContextPicoEdition extends WebAppContext {
    private final PicoContainer parentContainer;

    public WebAppContextPicoEdition(PicoContainer parentContainer) {
             super(null,null,new ServletHandlerPicoEdition(parentContainer),null);
        this.parentContainer = parentContainer;

    }

    protected void loadConfigurations() throws Exception {
        super.loadConfigurations();
        Configuration[]  configurations = getConfigurations();
        for (int i = 0; i < configurations.length; i++) {
            if (configurations[i] instanceof WebXmlConfiguration) {
                configurations[i] = new WebXmlConfigurationPicoEdition(parentContainer);
            }
        }
        setConfigurations(configurations);
    }


}

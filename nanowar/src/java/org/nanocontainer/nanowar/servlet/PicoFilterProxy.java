package org.nanocontainer.nanowar.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.nanocontainer.nanowar.ServletContainerFinder;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;

/**
* <p>
* A Filter which delegates to another Filter which is registered in a PicoContainer,
* in any of the web scopes, context, session or request.
*</p>
*
* <p>The delegate Filter must be registered via the "delegate-key" or "delegate-class" 
* init-params of this Filter.  
*
* <p>The initialization is done lazily, using the <code>init-type</code> init-param
* to control it.  Allowed values are:
* <ul>
*   <li>"context": will call init() on the filter only once</li>
*   <li>"request": will re-init it at every request</li>
*   <li>"never": will never init it</li>
*</ul>
*The default is "context".
* </p>
* 
* <p>The lookup in the PicoContainer is by default done for each request, but you
* can control that behaviour with the <code>lookup-only-once</code> init parameter.
* If set to "true", then PicoFilterProxy will only lookup your delegate filter
* at the first request.
* </p>
* 
* <p><b>Note</b>: Be aware that any dependency on your filter, in this setup, will stay
* referenced by your filter for its whole lifetime, eventhough this dependency
* might have been set up at request level in your composer!
* </p>
*
* @author Gr�gory Joseph
* @author Mauro Talevi
*/
public class PicoFilterProxy implements Filter {
    private static final int INIT_CONTEXT = 1;
    private static final int INIT_REQUEST = 4;
    private static final int INIT_NEVER = 7;
    private static final Map contextTypes = new HashMap();

    static {
        contextTypes.put(null, new Integer(INIT_CONTEXT));
        contextTypes.put("context", new Integer(INIT_CONTEXT));
        contextTypes.put("request", new Integer(INIT_REQUEST));
        contextTypes.put("never", new Integer(INIT_NEVER));
    };

    private int initTypeValue;
    private boolean lookupOnlyOnce;
    private FilterConfig filterConfig;
    private Filter delegate;
    private ServletContainerFinder containerFinder;

    /**
     * {@inheritDoc}
     *  @see Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        initTypeValue = toInitTypeValue(filterConfig.getInitParameter("init-type"));        
        lookupOnlyOnce = new Boolean(filterConfig.getInitParameter("lookup-only-once")).booleanValue();
    }

    /**
     * {@inheritDoc}
     * @see Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (delegate == null || !lookupOnlyOnce) {            
            delegate = lookupDelegate((HttpServletRequest) request);
            if (initTypeValue == INIT_CONTEXT) {
                initDelegate();
            }
        }
        if (initTypeValue == INIT_REQUEST) {
            initDelegate();
        }
        delegate.doFilter(request, response, filterChain);
    }

    /**
     * {@inheritDoc}
     * @see Filter#destroy()
     */
    public void destroy() {
        if (delegate != null) {
            delegate.destroy();
        }
    }
    
    /**
     * Converts 'init-type' param to an int value
     * @param initType
     * @return An int representing the init type value
     * @throws ServletException
     */
    private int toInitTypeValue(String initType) throws ServletException {
        Integer i = (Integer) contextTypes.get(initType);
        if (i == null) {
            throw new ServletException("Valid values for init-type are " + contextTypes.keySet());
        }
        return i.intValue();
    }

    /**
     * Looks up delegate Filter in PicoContainer found in any of the web scopes.
     * @param request the HttpServletRequest used to find the PicoContainer
     * @return A Filter 
     * @throws ServletException
     */
    private Filter lookupDelegate(HttpServletRequest request) throws ServletException {
        PicoContainer pico = findContainer(request);
        String delegateClassName = filterConfig.getInitParameter("delegate-class");
        String delegateKey = filterConfig.getInitParameter("delegate-key");
        Filter filter = null;
        if (delegateClassName != null) {
            try {
                Class delegateClass = getClassLoader().loadClass(delegateClassName);
                filter = (Filter) pico.getComponentInstanceOfType(delegateClass);
            } catch (ClassNotFoundException e) {
                throw new PicoInitializationException("Cannot load " + delegateClassName, e);
            }
        } else if (delegateKey != null) {
            filter = (Filter) pico.getComponentInstance(delegateKey);
        } else {
            throw new PicoInitializationException("You must specify one of delegate-class or delegate-key in the filter config, and you must register the corresponding component in your PicoContainer");
        }

        if (filter == null) {
            throw new PicoInitializationException("Cannot find delegate for class " + delegateClassName + " or key "+ delegateKey);
        }
        return filter;
    }

    /**
     * Finds PicoContainer via the ServletContainerFinder 
     * @param request the HttpServletRequest
     * @return A PicoContainer 
     * @see ServletContainerFinder
     */
    private PicoContainer findContainer(HttpServletRequest request) {
        if (containerFinder == null) {
            // lazy initialisation
            containerFinder = new ServletContainerFinder();
        }
        return containerFinder.findContainer(request);
    }

    private ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }
    
    private void initDelegate() throws ServletException {
        if (delegate == null) {
            throw new IllegalStateException("Delegate filter was not set up");
        }
        delegate.init(filterConfig);
    }

}

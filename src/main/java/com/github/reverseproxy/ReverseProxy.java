/* Copyright © 2010-2011 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.reverseproxy;

import java.io.*;
import java.net.*;

import org.eclipse.jetty.server.*;

/**
 * The main class which acts as a facade for the Reverse Proxy.
 */
public final class ReverseProxy {

    private final Server jettyServer;
    private final int port;
    private final ReverseProxyJettyHandler handler;

    /**
     * Constructor. This will find a free port, bind to it and start the server
     * up before it returns.
     * 
     * @param handler
     *            The {@link ReverseProxyJettyHandler} to use.
     */
    public ReverseProxy(ReverseProxyJettyHandler handler) {
        this(handler, getFreePort());
    }

    /**
     * Constructor. This will find a free port, bind to it and start the server
     * up before it returns.
     * 
     * @param handler
     *            The {@link ReverseProxyJettyHandler} to use.
     * @param port
     *            The port to listen on. Expect startup errors if this port is
     *            not free.
     */
    public ReverseProxy(ReverseProxyJettyHandler handler, int port) {
        this.port = port;
        this.handler = handler;

        jettyServer = new Server(port);

    }

    /**
     * Start the server
     */
    public void start() {
        try {
            jettyServer.setHandler(handler);
            jettyServer.start();

        } catch (Exception e) {
            throw new ReverseProxySetupException("Error starting jetty on port " + port, e);

        }
    }

    /**
     * Shutdown the server
     */
    public void stop() {
        try {
            jettyServer.stop();
        } catch (Exception e) {
            throw new ReverseProxySetupException("Error shutting down jetty", e);
        }
    }

    /**
     * Get the base URL which the ClientDriver is running on.
     * 
     * @return The base URL, which will be like "http://localhost:xxxx". <br/>
     *         <b>There is no trailing slash on this</b>
     */
    public String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Gets a free port on localhost for binding to.
     * 
     * @see "http://chaoticjava.com/posts/retrieving-a-free-port-for-socket-binding/"
     * 
     * @return The port number.
     */
    public static int getFreePort() {

        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;

        } catch (IOException ioe) {
            throw new ReverseProxySetupException("IOException finding free port", ioe);
        }
    }

    /*
     * public static void main(String[] args) { Map<String, String> urlMappings
     * = new HashMap<String, String>(); urlMappings.put("/calendar/",
     * "https://www.google.com"); urlMappings.put("/rest/1.0",
     * "https://st-account.nokia.com");
     * 
     * ReverseProxy reverseProxy = new ReverseProxy(new
     * ReverseProxyJettyHandler(urlMappings), 9999); reverseProxy.start(); }
     */

}

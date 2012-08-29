/**
 * copyright © 2012 Shine Paul
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

import java.util.*;
import java.util.concurrent.*;

import org.apache.maven.plugin.*;

/**
 * When invoked, this goal starts an instance of reverseproxy.
 * 
 * @goal start
 * @phase pre-integration-test
 */
public class StartReverseProxy extends AbstractMojo {

    private static final String PACKAGE_NAME = StartReverseProxy.class.getPackage().getName();
    public static final String REVERSE_PROXY_CONTEXT_PROPERTY_NAME = PACKAGE_NAME + ".reverseproxy";

    /**
     * The port reverse proxy should run on.
     * 
     * @parameter expression="${reverseProxy.port}" default-value="27017"
     * @since 0.0.1
     */
    private int port;

    /**
     * Block immediately and wait until MongoDB is explicitly stopped (eg:
     * {@literal <ctrl-c>}). This option makes this goal similar in spirit to
     * something like jetty:run, useful for interactive debugging.
     * 
     * @parameter expression="${reverseProxy.wait}" default-value="false"
     * @since 0.0.1
     */
    private boolean wait;

    /**
     * @parameter expression="${reverseProxy.urlMappings}" default-value=""
     * @since 0.0.1
     */
    private String urlMappingList;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {

        final ReverseProxy reverseProxy = new ReverseProxy(new ReverseProxyJettyHandler(getUrlMappings()), port);
        reverseProxy.start();
        /*
         * new Thread(new Runnable() { public void run() { reverseProxy.start();
         * } }).start();
         */

        if (wait) {
            while (true) {
                try {
                    TimeUnit.MINUTES.sleep(5);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        getPluginContext().put(REVERSE_PROXY_CONTEXT_PROPERTY_NAME, reverseProxy);

    }

    private Map<String, String> getUrlMappings() {
        Map<String, String> urlMappings = new HashMap<String, String>();
        String[] mappings = urlMappingList.split(",");
        for (String mapping : mappings) {
            String[] urlToMap = mapping.split("\\|");
            urlMappings.put(urlToMap[0], urlToMap[1]);
        }
        return urlMappings;
    }

}

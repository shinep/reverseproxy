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
package com.nokia.music.reverseproxy;

import org.apache.maven.plugin.*;

/**
 * When invoked, this goal stops an instance of mojo that was started by this
 * plugin.
 * 
 * @goal stop
 * @phase post-integration-test
 */
public class StopReverseProxy extends AbstractMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        Object reverseProxy = getPluginContext().get(StartReverseProxy.REVERSE_PROXY_CONTEXT_PROPERTY_NAME);

        if (reverseProxy != null) {
            ((ReverseProxy) reverseProxy).stop();
        } else {
            throw new MojoFailureException("No reverse proxy process found, it appears reverseproxy:start was not called");
        }
    }

}

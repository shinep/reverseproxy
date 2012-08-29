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

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.io.*;
import org.apache.commons.lang.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;

public class ReverseProxyJettyHandler extends AbstractHandler {

    private final String proxyHost;
    private final int proxyPort;

    private final Map<String, String> urlMappings;

    public ReverseProxyJettyHandler(Map<String, String> urlMappings) {
        this.urlMappings = urlMappings;
        this.proxyHost = "nokes.nokia.com";
        this.proxyPort = 8080;
    }

    public ReverseProxyJettyHandler(Map<String, String> urlMappings, String proxyHost, int proxyPort) {
        this.urlMappings = urlMappings;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        String requestPath = request.getRequestURI();
        String queryString = request.getQueryString();

        String method = request.getMethod();
        String requestBody = IOUtils.toString(request.getReader());
        requestBody = URLDecoder.decode(requestBody, "UTF-8"); //TODO

        String urlWithFullPath = StringUtils.isEmpty(queryString) ? requestPath : requestPath + "?" + queryString;
        String forwardUrl = getForwardUrl(urlWithFullPath);
        if (forwardUrl == null) {
            throw new IOException("Invalid forwardurl for : " + requestPath);
        }

        URLConnection urlConnection = getUrlConnection(request, method, requestBody, forwardUrl);
        writeResponse(urlConnection, response);

    }

    private URLConnection getUrlConnection(HttpServletRequest request, String method, String requestBody, String forwardUrl) throws MalformedURLException, IOException, ProtocolException {
        final URL url = new URL(forwardUrl);
        final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

        URLConnection urlConnection = url.openConnection(proxy);

        Map<String, String> requestHeaders = getRequestHeaders(request);
        requestHeaders.put("Host", urlConnection.getURL().getHost());

        ((HttpURLConnection) urlConnection).setRequestMethod(method);

        if (forwardUrl.startsWith("https")) {
            SSLSocketFactory sslSocketFactory = null;
            try {
                sslSocketFactory = getSSLSocketFactory();
            } catch (KeyManagementException e) {
                throw new IOException("Exception caught while setting up SSL props : ", e);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Exception caught while setting up SSL props : ", e);
            }
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
        }

        urlConnection.setDoInput(true);

        setHeaders(urlConnection, requestHeaders);

        setDoOutput(method, requestBody, urlConnection);

        return urlConnection;
    }

    private void writeResponse(URLConnection urlConn, HttpServletResponse response) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) urlConn;

        Map<String, List<String>> headers = urlConnection.getHeaderFields();
        for (String headerKey : headers.keySet()) {
            if (!StringUtils.isEmpty(headerKey)) {
                response.setHeader(headerKey, StringUtils.join(headers.get(headerKey), ","));
            }
        }

        int responseCode = urlConnection.getResponseCode();

        InputStream inputStream;
        if (responseCode >= 400) {
            inputStream = urlConnection.getErrorStream();
        } else {
            inputStream = urlConnection.getInputStream();
        }
        response.setStatus(responseCode);

        IOUtils.copy(inputStream, response.getOutputStream());
        IOUtils.closeQuietly(inputStream);
    }

    private void setHeaders(URLConnection urlConnection, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            urlConnection.setRequestProperty(key, headers.get(key));
        }
    }

    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerKey = headerNames.nextElement();
            if (!headerKey.equalsIgnoreCase("Accept-Encoding") && !headerKey.equalsIgnoreCase("Host")) { //Encoding doesnt work???
                String headerValue = StringUtils.join(Collections.list(request.getHeaders(headerKey)), ",");
                requestHeaders.put(headerKey, headerValue);
            }
        }
        return requestHeaders;
    }

    private void setDoOutput(String method, String requestBody, URLConnection urlConnection) throws IOException {
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
            urlConnection.setDoOutput(true);
            if (!StringUtils.isEmpty(requestBody)) {
                IOUtils.write(requestBody, urlConnection.getOutputStream());
            }
        } else {
            urlConnection.setDoOutput(false);
        }
    }

    private SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(final java.security.cert.X509Certificate[] chain, final String authType) {
                    }

                    public void checkServerTrusted(final java.security.cert.X509Certificate[] chain, final String authType) {
                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }};

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        return sslSocketFactory;
    }

    private String getForwardUrl(String requestPath) {
        for (String uri : urlMappings.keySet()) {
            if (requestPath != null && requestPath.startsWith(uri)) {
                return urlMappings.get(uri) + requestPath;
            }
        }
        return null;
    }

}

/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ocpsoft.rewrite.test;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
public class HttpAction<T extends HttpRequest>
{
    private final HttpClient client;
    private final T request;
    private final HttpResponse response;
    private final HttpContext context;
    private final String baseUrl;
    private final String contextPath;
    private String responseContent;

    public HttpAction(final HttpClient client, final HttpContext context, final T request,
                      final HttpResponse response, final String baseUrl, final String contextPath)
    {
        this.client = client;
        this.request = request;
        this.context = context;
        this.response = response;
        this.baseUrl = baseUrl;
        this.contextPath = contextPath;
    }

    /**
     * Return the current full URL.
     */
    public String getCurrentURL()
    {
        HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(
                HttpCoreContext.HTTP_REQUEST);
        HttpHost currentHost = (HttpHost) context.getAttribute(
            HttpCoreContext.HTTP_TARGET_HOST);
        String currentUrl = currentHost.toURI() + currentReq.getURI();

        if (currentUrl.startsWith(baseUrl))
        {
            currentUrl = currentUrl.substring(baseUrl.length());
        }

        return currentUrl;
    }

    /**
     * Return the current URL excluding host or context root.
     */
    public String getCurrentContextRelativeURL()
    {
        if (!getCurrentURL().startsWith(getContextPath()))
        {
            throw new IllegalStateException("Cannot get relative URL for address outside context root [" + getCurrentURL()
                    + "]");
        }
        return getCurrentURL().substring(getContextPath().length());
    }

    /**
     * Return the URL up to and including the host and port.
     */
    public String getHost()
    {
        HttpHost currentHost = (HttpHost) context.getAttribute(
            HttpCoreContext.HTTP_TARGET_HOST);

        return currentHost.toURI();
    }

    public HttpClient getClient()
    {
        return client;
    }

    public T getRequest()
    {
        return request;
    }

    public HttpContext getContext()
    {
        return context;
    }

    public HttpResponse getResponse()
    {
        return response;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public List<String> getResponseHeaderValues(final String name)
    {
        List<String> result = new ArrayList<String>();
        Header[] headers = getResponse().getHeaders(name);
        for (Header header : headers) {
            result.add(header.getValue());
        }
        return result;
    }

    public int getStatusCode()
    {
        return response.getStatusLine().getStatusCode();
    }

    public String getResponseContent() throws IOException {
        if (responseContent == null)
        {
            HttpEntity entity = getResponse().getEntity();
            if (entity != null)
            {
                responseContent = toString(entity.getContent());
            }
        }
        return responseContent;
    }

    /**
     * Return a {@link String} containing the contents of the given {@link InputStream}
     */
    public static String toString(final InputStream stream)
    {
        StringBuilder out = new StringBuilder();
        try {
            final char[] buffer = new char[0x10000];
            Reader in = new InputStreamReader(stream, "UTF-8");
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.append(buffer, 0, read);
                }
            }
            while (read >= 0);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }
}
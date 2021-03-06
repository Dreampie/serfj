/*
 * Copyright 2010 Eduardo Yáñez Parareda
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
package net.sf.serfj;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.serfj.annotations.DoNotRenderPage;
import net.sf.serfj.config.ConfigFileIOException;
import net.sf.serfj.serializers.FileSerializer;
import net.sf.serfj.serializers.ObjectSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows the developer to render the predefined page for an action,
 * or to render the page she wants, or serialize an object to JSon, XML, or
 * whatever as a response.
 * 
 * @author Eduardo Yáñez
 */
public class ResponseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHelper.class);
    private ServletContext context;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private UrlInfo urlInfo;
    private String viewsPath;
    private String requestedPage;
    private Object object2Serialize;
    private Map<String, Object> params;
    private boolean notRenderPage = false;
    private File file;
    private String attachmentFilename;
    private String contentType;
    private Config config;
    
    /**
     * Constructor.
     */
    protected ResponseHelper(ServletContext context, HttpServletRequest request, HttpServletResponse response, UrlInfo urlInfo, String viewsPath) {
        this.context = context;
        this.request = request;
        this.response = response;
        this.urlInfo = urlInfo;
        this.viewsPath = viewsPath;
		try {
			this.config = new Config("/config/serfj.properties");
		} catch (ConfigFileIOException e) {
			LOGGER.error("Can't load framework configuration", e);
		}
        this.initParams();
    }

    /**
     * Gets {@link HttpServletResponse} object related to this request.
     */
    public HttpServletResponse getResponse() {
        return this.response;
    }

    /**
     * Gets {@link HttpServletRequest} object related to this request.
     */
    public HttpServletRequest getRequest() {
        return this.request;
    }

    /**
     * Gets {@link ServletContext} object related to this request.
     */
    public ServletContext getContext() {
        return this.context;
    }

    /**
     * If the method is annotated with {@link DoNotRenderPage}, marks the response
     * as not to render any page. If there isn't object to serialize in the
     * response and notRenderPage flag is set on, the framework will return a
     * HTTP status code 204 (No content), else a page will be rendered.
     * 
     * @param method
     *            Method to inspect.
     */
    public void notRenderPage(Method method) {
        this.notRenderPage = method.getAnnotation(DoNotRenderPage.class) != null;
    }

    /**
     * Renders the predefined page.
     * 
     * @throws IOException
     *             if the page doesn't exist.
     */
    public void renderPage() throws IOException {
        this.requestedPage = this.getPage();
    }

    /**
     * Renders some page within 'views' directory.
     * 
     * @param page
     *            The page could have an extension or not. If it doesn't have an
     *            extension, the framework first looks for page.jsp, then .html
     *            or .htm extension.
     * @throws IOException
     *             if the page doesn't exist.
     */
    public void renderPage(String page) throws IOException {
        this.renderPage(urlInfo.getResource(), page);
    }

    /**
     * Renders a page from a resource.
     * 
     * @param resource
     *            The name of the resource (bank, account, etc...). It must
     *            exists below /views directory.
     * @param page
     *            The page can have an extension or not. If it doesn't have an
     *            extension, the framework first looks for page.jsp, then with
     *            .html or .htm extension.
     * @throws IOException
     *             if the page doesn't exist.
     */
    public void renderPage(String resource, String page) throws IOException {
        // If page comes with an extension
        String path = "/" + this.viewsPath + "/";
        if (resource != null) {
            path += resource + "/";
        }
        if (page.indexOf('.') > 1) {
            this.requestedPage = path + page;
        } else {
            // Search a page with .jsp, .html or .htm extension
            this.requestedPage = this.searchPage(path + page);
        }
    }

    /**
     * Serialize an object. Serializer class used to process the object can be
     * known using ResponseHelper.getSerializer() method.
     * 
     * @param object
     *            Object to serialize.
     */
    public void serialize(Object object) {
        this.object2Serialize = object;
    }

    /**
     * Sets the file to send to the client. If a file is set then the framework will try to read it
     * and write it into the response using a FileSerializer.
     * 
     * @param file The file.
     */
    public void setFile(File file) {
        this.file = file;
    }
    
    /**
     * Sets the filename for the file that will be sent.
     * 
     * @param filename File name.
     */
    public void setAttachmentFilename(String filename) {
        this.attachmentFilename = filename;
    }
    
    /**
     * Sets the contentType in case there isn't another FileSerializer implementation.<br><br>
     * 
     * @param contentType Content type for the response header.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /**
     * Sets the file to send to the client. If a file is set then the framework will try to read it
     * and write it into the response using a FileSerializer.
     * 
     * @param file The file.
     * @param attachmentFilename Name for the file that will be sent.
     * @param contentType Content type for the response header.
     */
    public void setFile(File file, String attachmentFilename, String contentType) {
        this.file = file;
        this.attachmentFilename = attachmentFilename;
        this.contentType = contentType;
    }
    
    /**
     * Gets the URL extension, if any.
     * 
     * @return An extension or an empty String if there isn't anyone.
     */
    public String getExtension() {
        return this.urlInfo.getExtension();
    }

    /**
     * Gets the Serializer class, if any.
     * 
     * @return A String with the fully qualified name of the serializer class
     *         that will be used in case of needed to serialize an object in the
     *         response.
     */
    public String getSerializer() {
        return this.urlInfo.getSerializer();
    }

    /**
     * Gets a Map containing all the parameters in the query string, and all the
     * attributes in the request.
     */
    public Map<String, Object> getParams() {
        return this.params;
    }

    /**
     * Gets a the value of a parameter that came in the URL or in the request.
     */
    public Object getParam(String name) {
        return this.params.get(name);
    }

    /**
     * Gets the value of an Id for the current resource.<br>
     * <br>
     * /sessions/1 -> Id: 1, identifier of 'session'<br>
     * /sessions/1/users/2 -> Id: 2, identifier of 'users'<br>
     */
    public String getId() {
        return this.urlInfo.getId();
    }

    /**
     * Gets the value of an Id given its resource's name.<br>
     * <br>
     * /sessions/1 -> Id: 1, Resource: session<br>
     * /sessions/1/users/2 -> Id: 2, Resource: user<br>
     */
    public String getId(String resource) {
        return this.urlInfo.getId(resource);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> initParams() {
        params = new HashMap<String, Object>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        Enumeration<String> attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String name = attributeNames.nextElement();
            params.put(name, request.getAttribute(name));
        }
        return params;
    }

    protected void doResponse() throws IOException, ServletException {
        if (!response.isCommitted()) {
            if (urlInfo.getSerializer() == null) {
                if (this.notRenderPage) {
                    response.setStatus(HttpURLConnection.HTTP_NO_CONTENT);
                    response.getWriter().flush();
                } else {
                    if (requestedPage == null) {
                        requestedPage = this.getPage();
                    }
                    this.forward();
                }
            } else {
                if (this.object2Serialize == null) {
                    if (this.file != null) {
                        this.sendFile();
                    } else {
                        LOGGER.warn("There is not object to serialize, returning no content response code: {}", HttpURLConnection.HTTP_NO_CONTENT);
                        response.setCharacterEncoding(this.config.getString(Config.ENCODING));
                        response.setStatus(HttpURLConnection.HTTP_NO_CONTENT);
                        response.getWriter().flush();
                    }
                } else {
                    this.serialize();
                }
            }
        }
    }

    protected void serialize() throws IOException {
        try {
            LOGGER.debug("Serializing using {}", urlInfo.getSerializer());
            Class<?> clazz = Class.forName(urlInfo.getSerializer());
            LOGGER.debug("Creating a new instance of {}", urlInfo.getSerializer());
            ObjectSerializer serializer = (ObjectSerializer) clazz.newInstance();
            LOGGER.debug("Calling {}.serialize()", urlInfo.getSerializer());
            String serialized = serializer.serialize(this.object2Serialize);
            LOGGER.debug("Writing object in the response: {}", serialized);
            this.writeObject(serializer.getContentType(), serialized);
        } catch (Exception e) {
            LOGGER.error("Can't serialize object with {} serializer: {}", urlInfo.getSerializer(), e.getLocalizedMessage());
            throw new IOException(e.getLocalizedMessage());
        }
    }

    protected void sendFile() throws IOException {
        try {
            LOGGER.debug("Sending file using {}", urlInfo.getSerializer());
            Class<?> clazz = Class.forName(urlInfo.getSerializer());
            LOGGER.debug("Creating a new instance of {}", urlInfo.getSerializer());
            FileSerializer serializer = (FileSerializer) clazz.newInstance();
            this.writeFile(serializer);
        } catch (Exception e) {
            LOGGER.error("Can't serialize object with {} serializer: {}", urlInfo.getSerializer(), e.getLocalizedMessage());
            throw new IOException(e.getLocalizedMessage());
        }
    }

    protected void forward() throws IOException, ServletException {
        if (requestedPage == null || "".equals(requestedPage)) {
            throw new IOException("Page or Action doesn't exist");
        } else {
            try {
                request.setAttribute("identifiers", urlInfo.getIdentifiers());
                RequestDispatcher dispatcher = context.getRequestDispatcher(requestedPage);
                LOGGER.debug("Forwarding to {}", requestedPage);
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
                throw e;
            }
        }
    }

    protected void writeObject(String contentType, String serialized) throws IOException {
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setContentType(contentType);
        response.setCharacterEncoding(this.config.getString(Config.ENCODING));
        response.getWriter().write(serialized);
        response.getWriter().flush();
    }

    protected void writeFile(FileSerializer serializer) throws IOException {
        if (this.contentType == null) { 
            response.setContentType(serializer.getContentType());
        } else {
            response.setContentType(this.contentType);
        }
        if (this.attachmentFilename == null) {
            this.attachmentFilename = this.file.getName();
        }
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=" + this.attachmentFilename);
        response.setContentLength((int) this.file.length());
        serializer.sendFile(this.file, this.response.getOutputStream());
    }

    private String searchPage(String pageWithoutExtension) {
        // First trying with .jsp extension
        String page = pageWithoutExtension + ".jsp";
        try {
            if (!this.existsPage(page)) {
                // Trying with .html extension
                page = pageWithoutExtension + ".html";
                if (!this.existsPage(page)) {
                    // Trying with .htm extension
                    page = pageWithoutExtension + ".htm";
                    if (!this.existsPage(page)) {
                        page = "";
                    }
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.debug("URL for page is not well formed", e);
            page = "";
        }
        return page;
    }

    private String getPage() {
        String path = "/" + this.viewsPath + "/";
        if (urlInfo.getResource() != null) {
            path += urlInfo.getResource() + "/";
        }
        // The special case is the 'new' action, otherwise the page is the same
        // as the action
        if (urlInfo.getAction().equals(UrlInspector.NEW_METHOD)) {
            path += UrlInspector.NEW_ACTION;
        } else {
            path += urlInfo.getAction();
        }

        return this.searchPage(path);
    }

    /**
     * Checks if a given page exists in the container and could be served.
     * 
     * @param page
     *            Page requested.
     * @return true if exists, false otherwise.
     * @throws MalformedURLException
     *             if the page's URL is not right.
     */
    private Boolean existsPage(String page) throws MalformedURLException {
        // Searching the page...
        LOGGER.debug("Searching page [{}]...", page);
        LOGGER.debug("Page's real path is [{}]", this.context.getRealPath(page));
        File file = new File(this.context.getRealPath(page));
        Boolean exists = file.exists();
        LOGGER.debug("Page [{}]{}found", page, (exists ? " " : " not "));
        return exists;
    }
}

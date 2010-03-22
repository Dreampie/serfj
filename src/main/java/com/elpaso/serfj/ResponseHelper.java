package com.elpaso.serfj;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows the developer to render the predefined page for an action, or to render the
 * page she wants, or serialize an object to JSon, XML, or whatever as a response.
 *
 * @author Eduardo Yáñez
 */
public class ResponseHelper {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHelper.class);
    private ServletContext context;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private UrlInfo urlInfo;
    private String viewsPath;
    private String requestedPage;
    private Object object2Serialize;
    private Map<String, Object> params;

    /**
     * Constructor.
     */
	protected ResponseHelper(ServletContext context, HttpServletRequest request,
            HttpServletResponse response, UrlInfo urlInfo, String viewsPath) {
        this.context = context;
        this.request = request;
        this.response = response;
        this.urlInfo = urlInfo;
        this.viewsPath = viewsPath;
        this.initParams();
    }

    /**
     * Renders the predefined page.
     *
     * @throws IOException if the page doesn't exist.
     */
    public void renderPage()throws IOException {
        this.requestedPage = this.getPage();
    }

    /**
     * Renders some page within 'views' directory.
     *
     * @param page - The page could have an extension or not. If it doesn't have an extension, the framework
     * first looks for page.jsp, then .html or .htm extension.
     *
     * @throws IOException if the page doesn't exist.
     */
    public void renderPage(String page) throws IOException {
        this.renderPage(urlInfo.getResource(), page);
    }

    /**
     * Renders a page from a resource.
     *
     * @param resource - The name of the resource (bank, account, etc...). It must exists below /views directory.
     * @param page - The page can have an extension or not. If it doesn't have an extension, the framework
     * first looks for page.jsp, then with .html or .htm extension.
     *
     * @throws IOException if the page doesn't exist.
     */
    public void renderPage(String resource, String page) throws IOException {
        // If page comes with an extension
        String path = "/" + this.viewsPath + "/";
        if (resource != null) {
            path +=  resource + "/";
        }
        if (page.indexOf(".") > 1) {
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
     * @param object - Object to serialize.
     */
    public void serialize(Object object) {
        this.object2Serialize = object;
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
     * @return A String with the fully qualified name of the serializer class that will be used in case
     * of needed to serialize an object in the response.
     */
    public String getSerializer() {
        return this.urlInfo.getSerializer();
    }

    /**
     * Gets a Map containing all the parameters in the query string, and all the attributes in the request.
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
     * Gets a the value of an Id given its resource's name.
     * 
     * /sessions/1 -> Id: 1, Resource: session
     * /sessions/1/users/2 -> Id: 2, Resource: user
     */
    public String getId(String resource) {
    	return this.urlInfo.getId(resource);
    }
    
	@SuppressWarnings("unchecked")
    private Map<String, Object> initParams() {
    	params = new HashMap<String, Object>();
    	Enumeration<String> paramNames = request.getParameterNames(); 
    	while(paramNames.hasMoreElements()) {
    		String name = paramNames.nextElement(); 
    		params.put(name, request.getParameter(name));
    	}
    	Enumeration<String> attributeNames = request.getAttributeNames(); 
    	while(attributeNames.hasMoreElements()) {
    		String name = attributeNames.nextElement(); 
    		params.put(name, request.getAttribute(name));
    	}
        return params;
    }
    
    protected void doResponse() throws IOException, ServletException {
        if (!response.isCommitted()) {
            if (urlInfo.getSerializer() == null) {
                if (requestedPage == null) {
                    requestedPage = this.getPage();
                }
                this.forward();
            } else {
                if (this.object2Serialize == null) {
                    throw new IllegalStateException("There is not object to serialize, must " +
                            "set the object using ResponseHelper.serialize method");
                }
                this.serialize();
            }
        }
    }

    protected void serialize() throws IOException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Serializing using {}", urlInfo.getSerializer());
            }
            Class<?> clazz = Class.forName(urlInfo.getSerializer());
            Method serializeMethod = clazz.getMethod("serialize", new Class[] {Serializable.class});
            if (logger.isDebugEnabled()) {
                logger.debug("Calling {}.serialize", urlInfo.getSerializer());
            }
            String serialized = (String) serializeMethod.invoke(clazz.newInstance(), this.object2Serialize);
            Method contentTypeMethod = clazz.getMethod("getContentType");
            if (logger.isDebugEnabled()) {
                logger.debug("Calling {}.getContentType()", urlInfo.getSerializer());
            }
            String contentType = (String) contentTypeMethod.invoke(clazz.newInstance());
            this.writeObject(contentType, serialized);
        } catch (Exception e) {
            logger.error("Can't serialize object with {} serializer: {}",
                    urlInfo.getSerializer(), e.getLocalizedMessage());
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
                if (logger.isDebugEnabled()) {
                    logger.debug("Forwarding to {}", requestedPage);
                }
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                logger.error(e.getLocalizedMessage(), e);
                throw e;
            }
        }
    }

    protected void writeObject(String contentType, String serialized) throws IOException {
        response.setContentType(contentType);
        response.getWriter().write(serialized);
        response.getWriter().flush();
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
            if (logger.isDebugEnabled()) {
                logger.debug("URL for page is not well formed", e);
            }
            page = "";
        }
        return page;
    }

    private String getPage() {
        String path = "/" + this.viewsPath + "/";
        if (urlInfo.getResource() != null) {
            path +=  urlInfo.getResource() + "/";
        }
        // The special case is the 'new' action, otherwise the page is the same as the action
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
     * @param page - Page requested.
     * @return true if exists, false otherwise.
     * @throws MalformedURLException if the page's URL is not right.
     */
    private Boolean existsPage(String page) throws MalformedURLException {
        // Searching the page...
        if (logger.isDebugEnabled()) {
            logger.debug("Searching page [{}]...", page);
            logger.debug("Page's real path is [{}]", this.context.getRealPath(page));
        }
        File file = new File(this.context.getRealPath(page));
        Boolean exists = file.exists();
        if (logger.isDebugEnabled()) {
            logger.debug("Page [{}]{}found", page, (exists ? " " : " not "));
        }
        return exists;
    }
}
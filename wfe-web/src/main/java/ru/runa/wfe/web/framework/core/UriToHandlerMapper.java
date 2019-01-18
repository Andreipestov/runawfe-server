package ru.runa.wfe.web.framework.core;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @see ServletConfiguration
 * @author Dmitry Grigoriev (dimgel)
 */
public abstract class UriToHandlerMapper {

    /**
     * Helper for subclasses.
     */
    protected static class PathComponents {
        private final String data[];
        private int nextIndex = 0;
        private String current;

        /**
         * @param pathInfo Must be non-null.
         */
        public PathComponents(String pathInfo) {
            this.data = StringUtils.split(pathInfo, '/');
        }

        /**
         * Returns next path component, or empty string if no more components exist.
         * Never returns null, because switch(null) on caller side throws NPE.
         * INSENSITIVE to trailing slashes, since returns "" for both missing and empty path component.
         */
        public String next() throws Exception {
            current = (nextIndex < data.length) ? URLDecoder.decode(data[nextIndex++], "UTF-8") : "";
            return current;
        }

        /**
         * Use this to get path parameter value, like this:
         *
         * <pre>
         * switch(pathComponents.next()) {
         *     case "":
         *         return null;  // Send error 404 if parameter is mandatory.
         *     default:
         *         pathParams.put("id", pathComponents.current());
         *         ...  // Switch on next subcomponent, or return handler.
         * }
         * </pre>
         */
        public String current() {
            return current;
        }
    }

    /**
     * ATTENTION! RequestHandler is stateful, so this method must create and return NEW instance on each call.
     *
     * @param uri Not null. Combined values of HttpServletRequest.getServletPath() and getPathInfo(). "/" for root URI.
     * @param pathParams Not null. Output parameter: here implementation must store path parameters it encounters during URI parsing.
     * @param params Input parameters: there should be parameters coming from the Web Interface.
     * @return NUll means error 404.
     * @throws Exception Means error 400.
     */
    protected abstract RequestHandler createHandler(RequestMethod method, String uri, Map<String, String> pathParams, Map<String, String[]> params) throws Exception;

    /**
     * ATTENTION! RequestHandler is stateful, so this method must create and return NEW instance on each call.
     *
     * Default implementation creates and returns DefaultErrorHandler.
     *
     * @param hrq Provided for request headers analysis (Accept, etc.).
     * @param status May be 400, 404, 500.
     * @return Must be non-null.
     *         Must take no params; should extend {@code RequestHandler<Object>}.
     *         Neither {@link RequestHandler#requestMethod} nor {@link RequestHandler#params} members will be filled by framework.
     *         Should pass null to RequestHandler's {@code acceptMethods} constructor, it will be ignored anyway.
     * @throws Exception Will be ignored.
     */
    @SuppressWarnings({"unused", "RedundantThrows", "WeakerAccess"})
    protected RequestHandler createErrorHandler(HttpServletRequest hrq, int status) throws Exception {
        return new DefaultErrorHandler(status);
    }
}

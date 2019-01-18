package ru.runa.wfe.web.framework.core;

import lombok.extern.apachecommons.CommonsLog;
import lombok.val;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * Entry point. Must be placed into web.xml.
 * Has one mandatory servlet init parameter "configurationClass" -- a full class name of {@link ServletConfiguration} subclass.
 *
 * @see ServletConfiguration
 * @author Dmitry Grigoriev (dimgel)
 */
@CommonsLog
public class Servlet extends HttpServlet {

    protected ServletConfiguration configuration;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            //noinspection unchecked
            val configClass = (Class<ServletConfiguration>) Class.forName(config.getInitParameter("configurationClass"));
            this.configuration = configClass.newInstance();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest hrq, HttpServletResponse hre) {
        RequestMethod method;
        RequestHandler handler;
        try {
            method = RequestMethod.valueOf(hrq.getMethod());
            String uri = getRequestUri(hrq);
            val pathParams = new HashMap<String, String>();
            val params = hrq.getParameterMap();
            handler = configuration.uriToHandlerMapper.createHandler(method, uri, pathParams, params);
            if (handler == null) {
                sendError(hrq, hre, HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!handler.acceptMethods.contains(method)) {
                throw new Exception("Request method is not acceptable");
            }

            //noinspection unchecked
            handler.params = configuration.requestParamsParser.parse(pathParams, hrq.getParameterMap(), handler);
        } catch (Throwable e) {
            log.error("Request dispatcher or parameter parser failed, responding error 400", e);
            sendError(hrq, hre, HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            handler.requestMethod = method;
            handler.httpServletRequest = hrq;
            handler.httpServletResponse = hre;
            handler.execute();
            hre.flushBuffer();
        } catch (Throwable e) {
            log.error("Request handler failed, responding error 500", e);
            sendError(hrq, hre, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected String getRequestUri(HttpServletRequest hrq) {
        String uri = hrq.getServletPath();
        String pathInfo = hrq.getPathInfo();
        if (pathInfo != null) {
            uri += pathInfo;
        }
        return uri;
    }

    @SuppressWarnings("WeakerAccess")
    protected void sendError(HttpServletRequest hrq, HttpServletResponse hre, int status) {
        try {
            val handler = configuration.uriToHandlerMapper.createErrorHandler(hrq, status);
            handler.httpServletRequest = hrq;
            handler.httpServletResponse = hre;
            handler.execute();
            hre.flushBuffer();
        } catch (Throwable e2) {
            // createErrorHandler() failed, I/O error, or response is already committed. Nothing we can do.
        }
    }
}

/*
 * This file is part of the RUNA WFE project.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; version 2.1
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ru.runa.wf.web.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import ru.runa.common.web.Commons;
import ru.runa.wfe.commons.ApplicationContextFactory;
import ru.runa.wfe.commons.ClassLoaderUtil;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.commons.web.AjaxCommand;
import ru.runa.wfe.commons.xml.XmlUtils;
import ru.runa.wfe.user.User;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class AjaxCommandServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog(AjaxCommandServlet.class);
    private static final String CONFIG = "ajax.commands.xml";
    private static final String COMMAND_ELEMENT = "command";
    private static final String NAME_ATTR = "name";
    private static final String CLASS_ATTR = "class";
    private static final Map<String, Class<? extends AjaxCommand>> definitions = Maps.newHashMap();

    static {
        registerDefinitions(ClassLoaderUtil.getAsStream(CONFIG, AjaxCommandServlet.class));
        try {
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + SystemProperties.RESOURCE_EXTENSION_PREFIX + CONFIG;
            Resource[] resources = ClassLoaderUtil.getResourcePatternResolver().getResources(pattern);
            for (Resource resource : resources) {
                registerDefinitions(resource.getInputStream());
            }
        } catch (IOException e) {
            log.error("unable load wfe.custom ajax command definitions", e);
        }
    }

    private static void registerDefinitions(InputStream inputStream) {
        try {
            Preconditions.checkNotNull(inputStream);
            Document document = XmlUtils.parseWithoutValidation(inputStream);
            Element root = document.getRootElement();
            List<Element> elements = root.elements(COMMAND_ELEMENT);
            for (Element element : elements) {
                String name = element.attributeValue(NAME_ATTR);
                try {
                    String className = element.attributeValue(CLASS_ATTR);
                    Class<? extends AjaxCommand> commandClass = (Class<? extends AjaxCommand>) ClassLoaderUtil.loadClass(className);
                    // test creation
                    ApplicationContextFactory.createAutowiredBean(commandClass);
                    definitions.put(name, commandClass);
                    log.debug("Registered command '" + name + "' as " + commandClass);
                } catch (Throwable e) {
                    log.warn("Unable to create command " + name, e);
                }
            }
            inputStream.close();
        } catch (Exception e) {
            log.error("unable load ajax command definitions", e);
        }
    }

    protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.debug("Got ajax request: " + request.getQueryString());
        try {
            String command = request.getParameter("command");
            User user = Commons.getUser(request.getSession());
            Class<? extends AjaxCommand> ajaxCommandClass = definitions.get(command);
            AjaxCommand ajaxCommand;
            if (ajaxCommandClass == null) {
                if (ApplicationContextFactory.getContext().containsBean(command)) {
                    ajaxCommand = ApplicationContextFactory.getContext().getBean(command, AjaxCommand.class);
                } else {
                    log.error("Request not handled, unknown command '" + command + "'");
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            } else {
                ajaxCommand = ApplicationContextFactory.createAutowiredBean(ajaxCommandClass);
            }
            ajaxCommand.execute(user, request, response);
        } catch (Exception e) {
            log.error("command", e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(request, response);
    }
}

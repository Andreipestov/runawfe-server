package ru.runa.wfe.web.config;

import lombok.val;
import ru.runa.wfe.web.api.GetMyTasks;
import ru.runa.wfe.web.api.GetProcessDefs;
import ru.runa.wfe.web.api.GetProcessForm;
import ru.runa.wfe.web.api.GetProcesses;
import ru.runa.wfe.web.framework.core.RequestHandler;
import ru.runa.wfe.web.framework.core.RequestMethod;
import ru.runa.wfe.web.framework.core.UriToHandlerMapper;
import ru.runa.wfe.web.framework.extra.JspHandler;
import ru.runa.wfe.web.framework.extra.RedirectHandler;

import java.util.Map;

public class UriToHandlerMapperImpl extends UriToHandlerMapper {

    @Override
    protected RequestHandler createHandler(RequestMethod method, String uri, Map<String, String> pathParams, Map<String, String[]> params) throws Exception {
        // Special cases not covered by PathComponents (which is insensitive to trailing slash).
        if (uri.isEmpty() || uri.equals("/ui2")) {
            return new RedirectHandler("/wfe/ui2/");
        }

        val pathComponents = new PathComponents(uri);
        switch (pathComponents.next()) {
            case "api": switch (pathComponents.next()) {
                case "myTasks":
                    return new GetMyTasks();
                case "processDefs":
                    return new GetProcessDefs();
                case "processForm":
                    return new GetProcessForm();
                case "processes":
                    return new GetProcesses();
                default:
                    return null;
            }
            case "ui2": switch (pathComponents.next()) {
                case "":
                    // TODO This is where compiled template engine could allow us to get rid of string literals & dynamic dispatch.
                    return new JspHandler("ui2/page/index.jsp");  // SPA
                case "myTasks":
                    return new JspHandler("ui2/page/myTasks.jsp");
                case "processDefs":
                    return new JspHandler("ui2/page/processDefs.jsp");
                case "processes":
                    return new JspHandler("ui2/page/processes.jsp");
                default:
                    return null;
            }
            default:
                return null;
        }
    }
}

package ru.runa.wfe.web.api;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.runa.common.web.Commons;
import ru.runa.wf.web.ftl.component.GenerateHtmlForVariable;
import ru.runa.wf.web.ftl.component.GenerateHtmlForVariableContext;
import ru.runa.wf.web.ftl.component.GenerateHtmlForVariableResult;
import ru.runa.wfe.service.client.DelegateDefinitionVariableProvider;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.user.User;
import ru.runa.wfe.var.VariableProvider;
import ru.runa.wfe.var.dto.WfVariable;
import ru.runa.wfe.var.format.VariableFormat;
import ru.runa.wfe.web.framework.extra.JsonHandler;
import ru.runa.wfe.web.reflection.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class GetProcessForm extends JsonHandler<GetProcessForm, JsonHandler.ListResponse<GetProcessForm.Row>> {
    protected final Log log = LogFactory.getLog(getClass());
    private String processId;

    public GetProcessForm() {
        super(acceptGet, GetProcessForm.class);
    }

    @Getter
    static class Row {
        String name;
        String form = "";
    }

    @Override
    protected ListResponse<Row> executeImpl() {
        val classPath = "ru.runa.wf.web.ftl.component.";
        val user = Commons.getUser(httpServletRequest.getSession());
        val definitionService = Delegates.getDefinitionService();
        val processDefinition = definitionService.getProcessDefinition(user, Long.valueOf(processId));
        val interaction = definitionService.getStartInteraction(user, Long.valueOf(processId));
        val variableProvider = new DelegateDefinitionVariableProvider(user, Long.valueOf(processId));
        return new ListResponse<Row>(user, 1) {{
            val row = new Row();
            row.name = processDefinition.getName();

            if (interaction.hasForm()) {
                String interactionFormData = new String(Delegates.getDefinitionService().getStartInteraction(user, Long.valueOf(processId)).getFormData(), Charsets.UTF_8);
                List<String> nameTamplatesList = getNameTamplates(interactionFormData);

                for (String nameTemplate : nameTamplatesList) {
                    List<String> dataToServer = getDataForServer(nameTemplate);

                    String template = dataToServer.get(0);
                    if (template.equals("InputVariable")) {
                        WfVariable variable = variableProvider.getVariableNotNull(dataToServer.get(1));
                        VariableFormat variableFormat = variable.getDefinition().getFormatNotNull();
                        GenerateHtmlForVariableContext context = new GenerateHtmlForVariableContext(variable, 0L, false);
                        GenerateHtmlForVariableResult generatedResult = variableFormat.processBy(new GenerateHtmlForVariable(user), context);
                        String variablesHtml = generatedResult.content;

                        interactionFormData = interactionFormData.replace(nameTemplate, variablesHtml);
                    } else {
                        try {
                            Object o = ReflectionUtils.createInstanceClass(classPath + template);
                            List<TemplateModel> arguments = new ArrayList<>();

                            for (int i = 1; i < dataToServer.size(); i ++) {
                                arguments.add(new SimpleScalar(dataToServer.get(i)));
                            }

                            ReflectionUtils.invokeMethod(o, "init", new Object[] {user, variableProvider}, User.class, VariableProvider.class);
                            Object data = ReflectionUtils.invokeMethod(o, "exec", new Object[] {arguments}, List.class);

                            interactionFormData = interactionFormData.replace(nameTemplate, (String) data);
                        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
                            log.error(e.getMessage());
                        }
                    }
                }

                row.form = interactionFormData;
            } else {
                row.form = row.form + "Form is not exists.";
            }

            getRows().add(row);
        }};
    }

    private List<String> getNameTamplates(String interactionFormData) {
        List<String> list = new ArrayList<>();
        char[] chars = interactionFormData.toCharArray();
        int i = 0;
        while (i < chars.length) {
            if (chars[i] == '$' && chars[i + 1] == '{') {
                StringBuilder element = new StringBuilder();
                boolean inWord = true;
                int j = i;
                while (j < chars.length) {
                    element.append(chars[j]);

                    if (chars[j + 1] == '}') {
                        element.append(chars[j + 1]);
                        inWord = false;
                        break;
                    }

                    j++;
                }

                if (!inWord) {
                    list.add(element.toString());
                }

                i = j;
            } else {
                i++;
            }
        }

        return list;
    }

    private List<String> getDataForServer(String nameTemplateList) {
        List<String> dataToServer = new ArrayList<>();
        dataToServer.add(nameTemplateList.substring(nameTemplateList.indexOf("{") + 1, nameTemplateList.indexOf("(")));

        String[] arguments = nameTemplateList.substring(nameTemplateList.indexOf("(") + 1, nameTemplateList.indexOf(")")).split(",");
        for (String argument : arguments) {
            dataToServer.add(argument.trim().replace("\"", ""));
        }

        return dataToServer;
    }
}

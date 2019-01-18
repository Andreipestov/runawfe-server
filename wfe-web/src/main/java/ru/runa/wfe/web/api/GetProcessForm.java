package ru.runa.wfe.web.api;

import lombok.Getter;
import lombok.val;
import ru.runa.common.web.Commons;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.var.VariableDefinition;
import ru.runa.wfe.web.framework.extra.JsonHandler;
import ru.runa.wfe.web.template.FormComponentTemplate;

import java.util.Map;

public class GetProcessForm extends JsonHandler<GetProcessForm, JsonHandler.ListResponse<GetProcessForm.Row>> {

    private String processId;

    public GetProcessForm() {
        super(acceptGet, GetProcessForm.class);
    }

    @Getter
    static class Row {
        String name;
        String form;
    }

    @Override
    protected ListResponse<Row> executeImpl() {
        val user = Commons.getUser(httpServletRequest.getSession());
        val definitionService = Delegates.getDefinitionService();
        val processDefinition = definitionService.getProcessDefinition(user, Long.valueOf(processId));
        return new ListResponse<Row>(user, 1) {{
            val row = new Row();
            row.name = processDefinition.getName();
            row.form = "";

            Map<String, VariableDefinition> variables = definitionService.getStartInteraction(user, Long.valueOf(processId)).getVariables();
            if (variables.size() > 0) {
                for (VariableDefinition variableDefinition : variables.values()) {
                    String variableFormat = variableDefinition.getFormat();
                    String variableName = variableFormat.substring(variableFormat.lastIndexOf(".") + 1);
                    String componentHtml = FormComponentTemplate.valueOf(variableName).getValue();

                    row.form = row.form + variableDefinition.getName() + ": </br>" + String.format(componentHtml, variableDefinition.getName()) + "</br>";
                }
            } else {
                row.form = row.form + "Form is not exists.";
            }

            getRows().add(row);
        }};
    }
}

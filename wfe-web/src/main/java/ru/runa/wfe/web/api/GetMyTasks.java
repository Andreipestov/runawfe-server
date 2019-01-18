package ru.runa.wfe.web.api;

import lombok.Getter;
import lombok.val;
import ru.runa.common.web.Commons;
import ru.runa.wfe.commons.CalendarUtil;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.task.dto.WfTask;
import ru.runa.wfe.web.framework.extra.JsonHandler;

public class GetMyTasks extends JsonHandler<GetMyTasks, JsonHandler.ListResponse<GetMyTasks.Row>> {

    public GetMyTasks() {
        super(acceptGet, GetMyTasks.class);
    }

    @Getter
    static class Row {
        long id;
        String name;
        String description;
        String definitionName;
        String creationDate;
        String deadlineDate;
    }

    @Override
    protected ListResponse<Row> executeImpl() {
        val user = Commons.getUser(httpServletRequest.getSession());
        val oo = Delegates.getTaskService().getMyTasks(user, null);
        return new ListResponse<Row>(user, oo.size()) {{
            for (WfTask o : oo) {
                val row = new Row();
                row.id = o.getId();
                row.name = o.getName();
                row.description = o.getDescription();
                row.definitionName = o.getDefinitionName();
                row.creationDate = CalendarUtil.formatDateTime(o.getCreationDate());
                row.deadlineDate = CalendarUtil.formatDateTime(o.getDeadlineDate());
                getRows().add(row);
            }
        }};
    }
}

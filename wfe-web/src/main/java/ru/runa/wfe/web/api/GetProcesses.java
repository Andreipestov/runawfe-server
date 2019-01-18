package ru.runa.wfe.web.api;

import lombok.Getter;
import lombok.val;
import ru.runa.common.web.Commons;
import ru.runa.wfe.commons.CalendarUtil;
import ru.runa.wfe.execution.ExecutionStatus;
import ru.runa.wfe.execution.dto.WfProcess;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.web.framework.extra.JsonHandler;

public class GetProcesses extends JsonHandler<GetProcesses, JsonHandler.ListResponse<GetProcesses.Row>> {

    public GetProcesses() {
        super(acceptGet, GetProcesses.class);
    }

    @Getter
    static class Row {
        long id;
        String name;
        String startDate;
        String endDate;
        ExecutionStatus executionStatus;
    }
    
    @Override
    protected ListResponse<Row> executeImpl() {
        val user = Commons.getUser(httpServletRequest.getSession());
        val oo = Delegates.getExecutionService().getProcesses(user, null);
        return new ListResponse<Row>(user, oo.size()) {{
            for (WfProcess o : oo) {
                val row = new Row();
                row.id = o.getId();
                row.name = o.getName();
                row.startDate = CalendarUtil.formatDateTime(o.getStartDate());
                row.endDate = CalendarUtil.formatDateTime(o.getEndDate());
                row.executionStatus = o.getExecutionStatus();

                getRows().add(row);
            }
        }};
    }
    
}

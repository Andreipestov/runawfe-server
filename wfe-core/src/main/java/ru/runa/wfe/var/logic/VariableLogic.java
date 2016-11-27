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
package ru.runa.wfe.var.logic;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ru.runa.wfe.audit.AdminActionLog;
import ru.runa.wfe.audit.ProcessLogFilter;
import ru.runa.wfe.audit.ProcessLogs;
import ru.runa.wfe.audit.VariableCreateLog;
import ru.runa.wfe.audit.VariableDeleteLog;
import ru.runa.wfe.audit.VariableLog;
import ru.runa.wfe.audit.logic.AuditLogic;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.commons.Utils;
import ru.runa.wfe.commons.logic.WFCommonLogic;
import ru.runa.wfe.execution.ExecutionContext;
import ru.runa.wfe.execution.ExecutionVariableProvider;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.execution.ProcessDoesNotExistException;
import ru.runa.wfe.execution.ProcessPermission;
import ru.runa.wfe.lang.MultiTaskNode;
import ru.runa.wfe.lang.ProcessDefinition;
import ru.runa.wfe.task.Task;
import ru.runa.wfe.user.User;
import ru.runa.wfe.var.IVariableProvider;
import ru.runa.wfe.var.UserType;
import ru.runa.wfe.var.Variable;
import ru.runa.wfe.var.VariableCreator;
import ru.runa.wfe.var.VariableDefinition;
import ru.runa.wfe.var.VariableMapping;
import ru.runa.wfe.var.dao.VariableLoader;
import ru.runa.wfe.var.dao.VariableLoaderFromMap;
import ru.runa.wfe.var.dto.WfVariable;
import ru.runa.wfe.var.dto.WfVariableHistoryState;
import ru.runa.wfe.var.format.VariableFormatContainer;

/**
 * Process execution logic.
 *
 * @author Dofs
 * @since 2.0
 */
public class VariableLogic extends WFCommonLogic {
    @Autowired
    private AuditLogic auditLogic;
    @Autowired
    private VariableCreator variableCreator;

    public List<WfVariable> getVariables(User user, Long processId) throws ProcessDoesNotExistException {
        List<WfVariable> result = Lists.newArrayList();
        Process process = processDAO.getNotNull(processId);
        ProcessDefinition processDefinition = getDefinition(process);
        checkPermissionAllowed(user, process, ProcessPermission.READ);
        ExecutionContext executionContext = new ExecutionContext(processDefinition, process);
        for (VariableDefinition variableDefinition : processDefinition.getVariables()) {
            WfVariable variable = executionContext.getVariable(variableDefinition.getName(), false);
            if (!Utils.isNullOrEmpty(variable.getValue())) {
                result.add(variable);
            }
        }
        return result;
    }

    public WfVariableHistoryState getHistoricalVariables(User user, ProcessLogFilter filter) throws ProcessDoesNotExistException {
        if (filter.getCreateDateFrom() == null) {
            return getHistoricalVariableOnDate(user, filter);
        } else {
            return getHistoricalVariableOnRange(user, filter);
        }
    }

    public WfVariableHistoryState getHistoricalVariables(User user, ProcessLogFilter filter, Set<String> variables)
            throws ProcessDoesNotExistException {
        List<WfVariable> result = Lists.newArrayList();
        WfVariableHistoryState historicalVariables = getHistoricalVariables(user, filter);
        for (WfVariable wfVariable : historicalVariables.getVariables()) {
            if (variables.contains(wfVariable.getDefinition().getName())) {
                result.add(wfVariable);
            }
        }
        return new WfVariableHistoryState(result, historicalVariables.getSimpleVariablesChanged());
    }

    public WfVariable getVariable(User user, Long processId, String variableName) throws ProcessDoesNotExistException {
        Process process = processDAO.getNotNull(processId);
        ProcessDefinition processDefinition = getDefinition(process);
        ExecutionContext executionContext = new ExecutionContext(processDefinition, process);
        return executionContext.getVariable(variableName, true);
    }

    public WfVariable getTaskVariable(User user, Long processId, Long taskId, String variableName) {
        Task task = taskDAO.getNotNull(taskId);
        if (task.getIndex() == null) {
            return getVariable(user, processId, variableName);
        }
        Process process = processDAO.getNotNull(processId);
        ProcessDefinition processDefinition = getDefinition(process);
        MultiTaskNode node = (MultiTaskNode) processDefinition.getNodeNotNull(task.getNodeId());
        for (VariableMapping mapping : node.getVariableMappings()) {
            if (Objects.equal(mapping.getMappedName(), variableName) || variableName.startsWith(mapping.getMappedName() + UserType.DELIM)) {
                String mappedVariableName = variableName.replaceFirst(mapping.getMappedName(), mapping.getName()
                        + VariableFormatContainer.COMPONENT_QUALIFIER_START + task.getIndex() + VariableFormatContainer.COMPONENT_QUALIFIER_END);
                WfVariable variable = getVariable(user, processId, mappedVariableName);
                if (variable == null) {
                    return null;
                }
                VariableDefinition mappedDefinition = new VariableDefinition(variableName, null, variable.getDefinition());
                return new WfVariable(mappedDefinition, variable.getValue());
            }
        }
        return getVariable(user, processId, variableName);
    }

    public void updateVariables(User user, Long processId, Map<String, Object> variables) {
        Process process = processDAO.getNotNull(processId);
        // TODO check ProcessPermission.UPDATE
        checkPermissionAllowed(user, process, ProcessPermission.READ);
        ProcessDefinition processDefinition = getDefinition(process);
        ExecutionContext executionContext = new ExecutionContext(processDefinition, process);
        processLogDAO.addLog(new AdminActionLog(user.getActor(), AdminActionLog.ACTION_UPDATE_VARIABLES), process, null);
        executionContext.setVariableValues(variables);
    }

    private WfVariableHistoryState getHistoricalVariableOnRange(User user, ProcessLogFilter filter) {
        HashSet<String> simpleVariablesChanged = Sets.<String>newHashSet();
        {
            Map<Process, Map<String, Object>> processToVariables = Maps.newHashMap();
            for (Process loadingProcess = processDAO.getNotNull(filter.getProcessId()); loadingProcess != null; loadingProcess = getBaseProcess(user,
                    loadingProcess)) {
                loadVariablesForProcessFromLogs(user, loadingProcess, filter, processToVariables, simpleVariablesChanged);
            }
        }
        List<WfVariable> result = Lists.newArrayList();
        Map<String, WfVariable> toVariables = Maps.newHashMap();
        Date dateFrom = filter.getCreateDateFrom();
        filter.setCreateDateFrom(null);
        for (WfVariable wfVariable : getHistoricalVariableOnDate(user, filter).getVariables()) {
            toVariables.put(wfVariable.getDefinition().getName(), wfVariable);
        }
        filter.setCreateDateTo(dateFrom);
        for (WfVariable wfVariable : getHistoricalVariableOnDate(user, filter).getVariables()) {
            boolean variableIsStillDefined = toVariables.containsKey(wfVariable.getDefinition().getName());
            if (variableIsStillDefined) {
                if (!Objects.equal(wfVariable.getValue(), toVariables.get(wfVariable.getDefinition().getName()).getValue())) {
                    result.add(toVariables.get(wfVariable.getDefinition().getName()));
                }
                toVariables.remove(wfVariable.getDefinition().getName());
            } else {
                wfVariable.setValue(null);
                result.add(wfVariable);
            }
        }
        for (WfVariable wfVariable : toVariables.values()) {
            result.add(wfVariable);
        }
        return new WfVariableHistoryState(result, simpleVariablesChanged);
    }

    private WfVariableHistoryState getHistoricalVariableOnDate(User user, ProcessLogFilter filter) {
        List<WfVariable> result = Lists.newArrayList();
        Process process = processDAO.getNotNull(filter.getProcessId());
        ProcessDefinition processDefinition = getDefinition(process);
        checkPermissionAllowed(user, process, ProcessPermission.READ);
        Set<String> simpleVariablesChanged = Sets.newHashSet();
        VariableLoader loader = new VariableLoaderFromMap(getProcessStateOnTime(user, process, filter, simpleVariablesChanged));
        for (VariableDefinition variableDefinition : processDefinition.getVariables()) {
            WfVariable variable = loader.getVariable(processDefinition, process, variableDefinition.getName());
            if (!Utils.isNullOrEmpty(variable.getValue())) {
                result.add(variable);
            }
        }
        return new WfVariableHistoryState(result, simpleVariablesChanged);
    }

    public Map<Process, Map<String, Variable<?>>> getProcessStateOnTime(User user, Process process, ProcessLogFilter filter,
            Set<String> simpleVariablesChanged) {
        Map<Process, Map<String, Object>> processToVariables = Maps.newHashMap();
        for (Process loadingProcess = process; loadingProcess != null; loadingProcess = getBaseProcess(user, loadingProcess)) {
            loadVariablesForProcessFromLogs(user, loadingProcess, filter, processToVariables, simpleVariablesChanged);
        }
        Map<Process, Map<String, Variable<?>>> result = Maps.newHashMap();
        for (Process currentProcess : processToVariables.keySet()) {
            ProcessDefinition definition = getDefinition(currentProcess);
            Map<String, Object> processVariables = processToVariables.get(currentProcess);
            result.put(currentProcess, Maps.<String, Variable<?>>newHashMap());
            for (String variableName : processVariables.keySet()) {
                VariableDefinition variableDefinition = definition.getVariable(variableName, false);
                Object value = processVariables.get(variableName);
                if (value instanceof String) {
                    value = variableDefinition.getFormatNotNull().parse((String) value);
                }
                Variable<?> variable = variableCreator.create(currentProcess, variableDefinition, value);
                variable.setValue(new ExecutionContext(definition, currentProcess), value, variableDefinition.getFormatNotNull());
                result.get(currentProcess).put(variableName, variable);
            }
        }
        return result;
    }

    private void loadVariablesForProcessFromLogs(User user, Process process, ProcessLogFilter filter,
            Map<Process, Map<String, Object>> processToVariables, Set<String> simpleVariablesChanged) {
        filter.setProcessId(process.getId());
        ProcessLogs historyLogs = auditLogic.getProcessLogs(user, filter);
        HashMap<String, Object> processVariables = Maps.<String, Object>newHashMap();
        processToVariables.put(process, processVariables);
        for (VariableLog variableLog : historyLogs.getLogs(VariableLog.class)) {
            String variableName = variableLog.getVariableName();
            if (!(variableLog instanceof VariableCreateLog) || !Utils.isNullOrEmpty(((VariableCreateLog) variableLog).getVariableNewValue())) {
                simpleVariablesChanged.add(variableName);
            }
            if (variableLog instanceof VariableDeleteLog) {
                processVariables.remove(variableName);
                continue;
            }
            processVariables.put(variableName, variableLog.getVariableNewValue());
        }
    }

    private Process getBaseProcess(User user, Process process) {
        if (Strings.isNullOrEmpty(SystemProperties.getBaseProcessIdVariableName())) {
            return null;
        }
        IVariableProvider processVariableProvider = new ExecutionVariableProvider(new ExecutionContext(getDefinition(process), process));
        final Long baseProcessId = (Long) processVariableProvider.getValue(SystemProperties.getBaseProcessIdVariableName());
        if (baseProcessId == null) {
            return null;
        }
        return processDAO.getNotNull(baseProcessId);
    }
}

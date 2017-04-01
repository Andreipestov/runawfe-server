package ru.runa.report.web.tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.Lists;

import ru.runa.wfe.definition.dto.WfDefinition;
import ru.runa.wfe.lang.SwimlaneDefinition;
import ru.runa.wfe.presentation.BatchPresentation;
import ru.runa.wfe.presentation.BatchPresentationFactory;
import ru.runa.wfe.report.ReportParameterType.ReportParameterTypeVisitor;
import ru.runa.wfe.report.dto.ReportParameterDto;
import ru.runa.wfe.report.impl.ReportParameterModel;
import ru.runa.wfe.report.impl.ReportParameterModel.ListValuesData;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.user.User;

/**
 * Operation of parameter model creation.
 */
public class ReportParameterCreateModelOperation implements ReportParameterTypeVisitor<ReportParameterModel, ReportParameterDto> {

    private final User user;

    public ReportParameterCreateModelOperation(User user) {
        this.user = user;
    }

    @Override
    public ReportParameterModel onString(ReportParameterDto data) {
        return new ReportParameterModel(data);
    }

    @Override
    public ReportParameterModel onNumber(ReportParameterDto data) {
        return new ReportParameterModel(data);
    }

    @Override
    public ReportParameterModel onDate(ReportParameterDto data) {
        return new ReportParameterModel(data);
    }

    @Override
    public ReportParameterModel onUncheckedBoolean(ReportParameterDto data) {
        return new ReportParameterModel(data);
    }

    @Override
    public ReportParameterModel onCheckedBoolean(ReportParameterDto data) {
        ReportParameterModel model = new ReportParameterModel(data);
        model.setValue(Boolean.toString(true));
        return model;
    }

    @Override
    public ReportParameterModel onProcessNameOrNull(ReportParameterDto data) {
        ReportParameterModel model = new ReportParameterModel(data);
        List<WfDefinition> definitions =
                Delegates.getDefinitionService().getProcessDefinitions(user, BatchPresentationFactory.DEFINITIONS.createNonPaged(), false);
        if (definitions == null) {
            definitions = new ArrayList<WfDefinition>();
        }
        List<ReportParameterModel.ListValuesData> listData = new ArrayList<ReportParameterModel.ListValuesData>();
        listData.add(new ListValuesData("All BPs", null));
        for (WfDefinition definition : definitions) {
            listData.add(new ReportParameterModel.ListValuesData(definition.getName(), definition.getName()));
        }
        model.setListValues(listData);
        return model;
    }

    @Override
    public ReportParameterModel onSwimlane(ReportParameterDto data) {
        ReportParameterModel model = new ReportParameterModel(data);
        List<WfDefinition> definitions =
                Delegates.getDefinitionService().getProcessDefinitions(user, BatchPresentationFactory.DEFINITIONS.createNonPaged(), false);
        if (definitions == null) {
            definitions = new ArrayList<WfDefinition>();
        }
        Set<String> swimlanes = new TreeSet<String>();

        for (WfDefinition definition : definitions) {
            try {
                for (SwimlaneDefinition swimlane : Delegates.getDefinitionService().getSwimlaneDefinitions(user, definition.getId())) {
                    swimlanes.add(swimlane.getName());
                }
            } catch (Exception e) {
                // invalid, does not exist
            }
        }

        List<ReportParameterModel.ListValuesData> listData = new ArrayList<ReportParameterModel.ListValuesData>();
        listData.add(new ListValuesData("All swimlanes", null));
        for (String swimlane : swimlanes) {
            listData.add(new ListValuesData(swimlane, swimlane));
        }
        model.setListValues(listData);
        return model;
    }

    @Override
    public ReportParameterModel onActorId(ReportParameterDto data) {
        return createExecutorsSelectionModel(data, BatchPresentationFactory.ACTORS.createNonPaged(), false);
    }

    @Override
    public ReportParameterModel onGroupId(ReportParameterDto data) {
        return createExecutorsSelectionModel(data, BatchPresentationFactory.GROUPS.createNonPaged(), false);
    }

    @Override
    public ReportParameterModel onExecutorId(ReportParameterDto data) {
        return createExecutorsSelectionModel(data, BatchPresentationFactory.EXECUTORS.createNonPaged(), false);
    }

    @Override
    public ReportParameterModel onActorName(ReportParameterDto data) {
        return createExecutorsSelectionModel(data, BatchPresentationFactory.ACTORS.createNonPaged(), true);
    }

    @Override
    public ReportParameterModel onGroupName(ReportParameterDto data) {
        return createExecutorsSelectionModel(data, BatchPresentationFactory.GROUPS.createNonPaged(), true);
    }

    @Override
    public ReportParameterModel onExecutorName(ReportParameterDto data) {
        return createExecutorsSelectionModel(data, BatchPresentationFactory.EXECUTORS.createNonPaged(), true);
    }

    private ReportParameterModel createExecutorsSelectionModel(ReportParameterDto data, BatchPresentation batch, boolean nameSelection) {
        ReportParameterModel model = new ReportParameterModel(data);
        List<? extends Executor> executors = Delegates.getExecutorService().getExecutors(user, batch);
        if (executors == null) {
            executors = Lists.newArrayList();
        }
        List<ReportParameterModel.ListValuesData> listData = new ArrayList<ReportParameterModel.ListValuesData>();
        listData.add(new ListValuesData("All", null));
        for (Executor executor : executors) {
            Object selectionValue = nameSelection ? executor.getName() : executor.getId().toString();
            listData.add(new ListValuesData(executor.getName() + " (" + executor.getFullName() + ")", selectionValue));
        }
        model.setListValues(listData);
        return model;
    }
}
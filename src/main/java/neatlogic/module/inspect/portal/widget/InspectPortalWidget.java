/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.inspect.portal.widget;

import neatlogic.framework.portal.widget.core.IPortalWidget;
import neatlogic.framework.portal.widget.core.IPortalWidgetGroup;

public enum InspectPortalWidget implements IPortalWidget {
    inspectRecentIssue("inspectRecentIssue", "近期问题", 1, InspectPortalWidgetGroup.inspectGroup1),
    inspectRunningJob("inspectRunningJob", "执行中巡检", 2, InspectPortalWidgetGroup.inspectGroup1),
    inspectAbnormalAsset("inspectAbnormalAsset", "异常资产", 3, InspectPortalWidgetGroup.inspectGroup2),
    inspectQualityTrend("inspectQualityTrend", "问题趋势与通过率", 4, InspectPortalWidgetGroup.inspectGroup2),
    ;
    private final String value;
    private final String text;
    private final Integer sort;
    private final IPortalWidgetGroup group;

    InspectPortalWidget(String value, String text, Integer sort, IPortalWidgetGroup group) {
        this.value = value;
        this.text = text;
        this.sort = sort;
        this.group = group;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public Integer getSort() {
        return this.sort;
    }

    @Override
    public IPortalWidgetGroup getGroup() {
        return this.group;
    }
}

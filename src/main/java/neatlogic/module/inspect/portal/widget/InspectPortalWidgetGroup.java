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

import neatlogic.framework.portal.widget.core.IPortalWidgetGroup;

public enum InspectPortalWidgetGroup implements IPortalWidgetGroup {
    inspectGroup1("inspectGroup1", "巡检分组1", 1),
    inspectGroup2("inspectGroup2", "巡检分组2", 2),
    ;

    private final String value;
    private final String text;
    private final Integer sort;

    InspectPortalWidgetGroup(String value, String text, Integer sort) {
        this.value = value;
        this.text = text;
        this.sort = sort;
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
}

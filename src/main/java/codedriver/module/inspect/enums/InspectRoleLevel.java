package codedriver.module.inspect.enums;

import codedriver.framework.common.constvalue.IEnum;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public enum InspectRoleLevel implements IEnum {
    WARN("WARN", "警告"),
    CRITICAL("CRITICAL", "严重");
    private final String value;
    private final String text;

    InspectRoleLevel(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    @Override
    public List getValueTextList() {
        JSONArray array = new JSONArray();
        for (InspectRoleLevel level : values()) {
            array.add(new JSONObject() {
                {
                    this.put("value", level.getValue());
                    this.put("text", level.getText());
                }
            });
        }
        return array;
    }
}

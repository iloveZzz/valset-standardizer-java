package com.yss.valset.transfer.domain.form.model;

/**
 * 组件类型。
 */
public enum Component implements YssFormilyDsl.WireValue {
    FORM_LAYOUT("FormLayout"),
    FORM_GRID("FormGrid"),
    FORM_ITEM("FormItem"),
    INPUT("Input"),
    INPUT_TEXT_AREA("Input.TextArea"),
    INPUT_NUMBER("InputNumber"),
    SELECT("Select"),
    RADIO_GROUP("Radio.Group"),
    SWITCH("Switch"),
    DATE_PICKER("DatePicker"),
    DATE_RANGE_PICKER("DatePicker.RangePicker"),
    SLOT("Slot"),
    GROUP_HEADER("GroupHeader"),
    SUBMIT("Submit"),
    RESET("Reset"),
    AUTO_BUTTON_GROUP("AutoButtonGroup");

    private final String value;

    Component(String value) {
        this.value = value;
    }

    @Override
    public Object wireValue() {
        return value;
    }
}

package com.yss.valset.transfer.domain.form.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * 事件绑定。
 */
@Getter
public class EventBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String expression;

    public EventBinding(String name, String expression) {
        this.name = name;
        this.expression = expression;
    }
}

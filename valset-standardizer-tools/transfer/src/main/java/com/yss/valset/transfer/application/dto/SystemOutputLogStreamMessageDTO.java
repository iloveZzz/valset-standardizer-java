package com.yss.valset.transfer.application.dto;

/**
 * 系统输出日志流消息。
 */
public class SystemOutputLogStreamMessageDTO implements java.io.Serializable {

    private String type;

    private String source;

    private Object data;

    public SystemOutputLogStreamMessageDTO() {
    }

    public SystemOutputLogStreamMessageDTO(String type, String source, Object data) {
        this.type = type;
        this.source = source;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

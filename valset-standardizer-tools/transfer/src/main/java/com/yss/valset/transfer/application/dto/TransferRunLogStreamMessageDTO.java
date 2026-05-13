package com.yss.valset.transfer.application.dto;

/**
 * 文件收发运行日志 SSE 消息。
 */
public class TransferRunLogStreamMessageDTO implements java.io.Serializable {

    private final String type;
    private final String taskId;
    private final TransferRunLogViewDTO data;

    public TransferRunLogStreamMessageDTO(String type, String taskId, TransferRunLogViewDTO data) {
        this.type = type;
        this.taskId = taskId;
        this.data = data;
    }



    public String type() {
        return type;
    }

    public String taskId() {
        return taskId;
    }

    public TransferRunLogViewDTO data() {
        return data;
    }



    public String getType() {
        return type;
    }

    public String getTaskId() {
        return taskId;
    }

    public TransferRunLogViewDTO getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferRunLogStreamMessageDTO other = (TransferRunLogStreamMessageDTO) o;
        if (!java.util.Objects.equals(type, other.type)) {
            return false;
        }
        if (!java.util.Objects.equals(taskId, other.taskId)) {
            return false;
        }
        if (!java.util.Objects.equals(data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, taskId, data);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRunLogStreamMessageDTO[");
        sb.append("type=").append(type);
        sb.append(", taskId=").append(taskId);
        sb.append(", data=").append(data);
        sb.append(']');
        return sb.toString();
    }



}

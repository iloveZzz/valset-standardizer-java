package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 投递执行上下文。
 */
public class TransferContext {

    private final TransferObject transferObject;
    private final TransferRoute transferRoute;
    private final TransferTarget transferTarget;
    private final Map<String, Object> attributes;

    public TransferContext(TransferObject transferObject, TransferRoute transferRoute, TransferTarget transferTarget, Map<String, Object> attributes) {
        this.transferObject = transferObject;
        this.transferRoute = transferRoute;
        this.transferTarget = transferTarget;
        this.attributes = attributes;
    }



    public TransferObject transferObject() {
        return transferObject;
    }

    public TransferRoute transferRoute() {
        return transferRoute;
    }

    public TransferTarget transferTarget() {
        return transferTarget;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }



    public TransferObject getTransferObject() {
        return transferObject;
    }

    public TransferRoute getTransferRoute() {
        return transferRoute;
    }

    public TransferTarget getTransferTarget() {
        return transferTarget;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferContext other = (TransferContext) o;
        if (!java.util.Objects.equals(transferObject, other.transferObject)) {
            return false;
        }
        if (!java.util.Objects.equals(transferRoute, other.transferRoute)) {
            return false;
        }
        if (!java.util.Objects.equals(transferTarget, other.transferTarget)) {
            return false;
        }
        if (!java.util.Objects.equals(attributes, other.attributes)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(transferObject, transferRoute, transferTarget, attributes);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferContext[");
        sb.append("transferObject=").append(transferObject);
        sb.append(", transferRoute=").append(transferRoute);
        sb.append(", transferTarget=").append(transferTarget);
        sb.append(", attributes=").append(attributes);
        sb.append(']');
        return sb.toString();
    }



}

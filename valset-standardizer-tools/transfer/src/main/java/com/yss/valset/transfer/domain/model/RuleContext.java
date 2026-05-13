package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 规则执行上下文。
 */
public class RuleContext {

    private final RecognitionContext recognitionContext;
    private final ProbeResult probeResult;
    private final Map<String, Object> variables;

    public RuleContext(RecognitionContext recognitionContext, ProbeResult probeResult, Map<String, Object> variables) {
        this.recognitionContext = recognitionContext;
        this.probeResult = probeResult;
        this.variables = variables;
    }



    public RecognitionContext recognitionContext() {
        return recognitionContext;
    }

    public ProbeResult probeResult() {
        return probeResult;
    }

    public Map<String, Object> variables() {
        return variables;
    }



    public RecognitionContext getRecognitionContext() {
        return recognitionContext;
    }

    public ProbeResult getProbeResult() {
        return probeResult;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RuleContext other = (RuleContext) o;
        if (!java.util.Objects.equals(recognitionContext, other.recognitionContext)) {
            return false;
        }
        if (!java.util.Objects.equals(probeResult, other.probeResult)) {
            return false;
        }
        if (!java.util.Objects.equals(variables, other.variables)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(recognitionContext, probeResult, variables);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RuleContext[");
        sb.append("recognitionContext=").append(recognitionContext);
        sb.append(", probeResult=").append(probeResult);
        sb.append(", variables=").append(variables);
        sb.append(']');
        return sb.toString();
    }



}

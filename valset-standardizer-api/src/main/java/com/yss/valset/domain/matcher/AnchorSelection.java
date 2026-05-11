package com.yss.valset.domain.matcher;

import com.yss.valset.domain.model.SubjectRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnchorSelection {
    private SubjectRecord anchorSubject;
    private List<String> anchorPathNames;
    private String anchorPathText;
    private String reason;
}

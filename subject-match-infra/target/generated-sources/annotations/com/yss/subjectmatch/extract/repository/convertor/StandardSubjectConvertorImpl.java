package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.StandardSubject;
import com.yss.subjectmatch.extract.repository.entity.StandardSubjectPO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class StandardSubjectConvertorImpl implements StandardSubjectConvertor {

    @Override
    public StandardSubjectPO toPO(StandardSubject standardSubject) {
        if ( standardSubject == null ) {
            return null;
        }

        StandardSubjectPO standardSubjectPO = new StandardSubjectPO();

        standardSubjectPO.setStandardCode( standardSubject.getStandardCode() );
        standardSubjectPO.setStandardName( standardSubject.getStandardName() );
        standardSubjectPO.setParentCode( standardSubject.getParentCode() );
        standardSubjectPO.setParentName( standardSubject.getParentName() );
        standardSubjectPO.setLevel( standardSubject.getLevel() );
        standardSubjectPO.setRootCode( standardSubject.getRootCode() );
        standardSubjectPO.setSegmentCount( standardSubject.getSegmentCount() );
        standardSubjectPO.setPathText( standardSubject.getPathText() );
        standardSubjectPO.setNormalizedName( standardSubject.getNormalizedName() );
        standardSubjectPO.setNormalizedPathText( standardSubject.getNormalizedPathText() );
        standardSubjectPO.setPlaceholder( standardSubject.getPlaceholder() );

        return standardSubjectPO;
    }

    @Override
    public StandardSubject toDomain(StandardSubjectPO po) {
        if ( po == null ) {
            return null;
        }

        StandardSubject.StandardSubjectBuilder standardSubject = StandardSubject.builder();

        standardSubject.standardCode( po.getStandardCode() );
        standardSubject.standardName( po.getStandardName() );
        standardSubject.parentCode( po.getParentCode() );
        standardSubject.parentName( po.getParentName() );
        standardSubject.level( po.getLevel() );
        standardSubject.rootCode( po.getRootCode() );
        standardSubject.segmentCount( po.getSegmentCount() );
        standardSubject.pathText( po.getPathText() );
        standardSubject.normalizedName( po.getNormalizedName() );
        standardSubject.normalizedPathText( po.getNormalizedPathText() );
        standardSubject.placeholder( po.getPlaceholder() );

        return standardSubject.build();
    }

    @Override
    public List<StandardSubjectPO> toPO(List<StandardSubject> standardSubjects) {
        if ( standardSubjects == null ) {
            return null;
        }

        List<StandardSubjectPO> list = new ArrayList<StandardSubjectPO>( standardSubjects.size() );
        for ( StandardSubject standardSubject : standardSubjects ) {
            list.add( toPO( standardSubject ) );
        }

        return list;
    }
}

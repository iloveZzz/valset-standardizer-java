package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.MappingHint;
import com.yss.subjectmatch.extract.repository.entity.MappingHintPO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class MappingHintConvertorImpl implements MappingHintConvertor {

    @Override
    public MappingHintPO toPO(MappingHint mappingHint) {
        if ( mappingHint == null ) {
            return null;
        }

        MappingHintPO mappingHintPO = new MappingHintPO();

        mappingHintPO.setSource( mappingHint.getSource() );
        mappingHintPO.setNormalizedKey( mappingHint.getNormalizedKey() );
        mappingHintPO.setStandardCode( mappingHint.getStandardCode() );
        mappingHintPO.setStandardName( mappingHint.getStandardName() );
        mappingHintPO.setSupportCount( mappingHint.getSupportCount() );
        mappingHintPO.setConfidence( mappingHint.getConfidence() );

        return mappingHintPO;
    }

    @Override
    public MappingHint toDomain(MappingHintPO po) {
        if ( po == null ) {
            return null;
        }

        MappingHint.MappingHintBuilder mappingHint = MappingHint.builder();

        mappingHint.source( po.getSource() );
        mappingHint.normalizedKey( po.getNormalizedKey() );
        mappingHint.standardCode( po.getStandardCode() );
        mappingHint.standardName( po.getStandardName() );
        mappingHint.supportCount( po.getSupportCount() );
        mappingHint.confidence( po.getConfidence() );

        return mappingHint.build();
    }
}

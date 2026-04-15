package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.MappingSample;
import com.yss.subjectmatch.extract.repository.entity.MappingSamplePO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class MappingSampleConvertorImpl implements MappingSampleConvertor {

    @Override
    public MappingSamplePO toPO(MappingSample mappingSample) {
        if ( mappingSample == null ) {
            return null;
        }

        MappingSamplePO mappingSamplePO = new MappingSamplePO();

        mappingSamplePO.setOrgName( mappingSample.getOrgName() );
        mappingSamplePO.setOrgId( mappingSample.getOrgId() );
        mappingSamplePO.setExternalCode( mappingSample.getExternalCode() );
        mappingSamplePO.setExternalName( mappingSample.getExternalName() );
        mappingSamplePO.setStandardCode( mappingSample.getStandardCode() );
        mappingSamplePO.setStandardName( mappingSample.getStandardName() );
        mappingSamplePO.setStandardSystem( mappingSample.getStandardSystem() );
        mappingSamplePO.setSystemName( mappingSample.getSystemName() );

        return mappingSamplePO;
    }

    @Override
    public MappingSample toDomain(MappingSamplePO po) {
        if ( po == null ) {
            return null;
        }

        MappingSample.MappingSampleBuilder mappingSample = MappingSample.builder();

        mappingSample.orgName( po.getOrgName() );
        mappingSample.orgId( po.getOrgId() );
        mappingSample.externalCode( po.getExternalCode() );
        mappingSample.externalName( po.getExternalName() );
        mappingSample.standardCode( po.getStandardCode() );
        mappingSample.standardName( po.getStandardName() );
        mappingSample.standardSystem( po.getStandardSystem() );
        mappingSample.systemName( po.getSystemName() );

        return mappingSample.build();
    }

    @Override
    public List<MappingSamplePO> toPO(List<MappingSample> mappingSamples) {
        if ( mappingSamples == null ) {
            return null;
        }

        List<MappingSamplePO> list = new ArrayList<MappingSamplePO>( mappingSamples.size() );
        for ( MappingSample mappingSample : mappingSamples ) {
            list.add( toPO( mappingSample ) );
        }

        return list;
    }

    @Override
    public List<MappingSample> toDomain(List<MappingSamplePO> poList) {
        if ( poList == null ) {
            return null;
        }

        List<MappingSample> list = new ArrayList<MappingSample>( poList.size() );
        for ( MappingSamplePO mappingSamplePO : poList ) {
            list.add( toDomain( mappingSamplePO ) );
        }

        return list;
    }
}

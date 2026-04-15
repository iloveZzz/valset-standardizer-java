package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.extract.repository.entity.ParsedValuationDataPO;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class ParsedValuationDataConvertorImpl implements ParsedValuationDataConvertor {

    @Autowired
    private JsonStringMapper jsonStringMapper;

    @Override
    public ParsedValuationDataPO toPO(Long taskId, Long fileId, ParsedValuationData parsedValuationData) {
        if ( taskId == null && fileId == null && parsedValuationData == null ) {
            return null;
        }

        ParsedValuationDataPO parsedValuationDataPO = new ParsedValuationDataPO();

        if ( parsedValuationData != null ) {
            parsedValuationDataPO.setBasicInfoJson( jsonStringMapper.toJson( parsedValuationData.getBasicInfo() ) );
            parsedValuationDataPO.setHeadersJson( jsonStringMapper.toJson( parsedValuationData.getHeaders() ) );
            parsedValuationDataPO.setHeaderDetailsJson( jsonStringMapper.toJson( parsedValuationData.getHeaderDetails() ) );
            parsedValuationDataPO.setSubjectsJson( jsonStringMapper.toJson( parsedValuationData.getSubjects() ) );
            parsedValuationDataPO.setMetricsJson( jsonStringMapper.toJson( parsedValuationData.getMetrics() ) );
            parsedValuationDataPO.setWorkbookPath( jsonStringMapper.toJson( parsedValuationData.getWorkbookPath() ) );
            parsedValuationDataPO.setSheetName( jsonStringMapper.toJson( parsedValuationData.getSheetName() ) );
            parsedValuationDataPO.setHeaderRowNumber( parsedValuationData.getHeaderRowNumber() );
            parsedValuationDataPO.setDataStartRowNumber( parsedValuationData.getDataStartRowNumber() );
            parsedValuationDataPO.setTitle( jsonStringMapper.toJson( parsedValuationData.getTitle() ) );
        }
        parsedValuationDataPO.setTaskId( taskId );
        parsedValuationDataPO.setFileId( fileId );

        return parsedValuationDataPO;
    }

    @Override
    public ParsedValuationData toDomain(ParsedValuationDataPO po) {
        if ( po == null ) {
            return null;
        }

        ParsedValuationData.ParsedValuationDataBuilder parsedValuationData = ParsedValuationData.builder();

        parsedValuationData.basicInfo( jsonStringMapper.toMapStringString( po.getBasicInfoJson() ) );
        parsedValuationData.headers( jsonStringMapper.toListString( po.getHeadersJson() ) );
        parsedValuationData.headerDetails( jsonStringMapper.toListListString( po.getHeaderDetailsJson() ) );
        parsedValuationData.subjects( jsonStringMapper.toListSubjectRecord( po.getSubjectsJson() ) );
        parsedValuationData.metrics( jsonStringMapper.toListMetricRecord( po.getMetricsJson() ) );
        parsedValuationData.workbookPath( jsonStringMapper.toJson( po.getWorkbookPath() ) );
        parsedValuationData.sheetName( jsonStringMapper.toJson( po.getSheetName() ) );
        parsedValuationData.headerRowNumber( po.getHeaderRowNumber() );
        parsedValuationData.dataStartRowNumber( po.getDataStartRowNumber() );
        parsedValuationData.title( jsonStringMapper.toJson( po.getTitle() ) );

        return parsedValuationData.build();
    }
}

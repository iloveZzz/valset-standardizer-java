package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.SubjectMatchFileIngestLog;
import com.yss.subjectmatch.domain.model.SubjectMatchFileSourceChannel;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchFileIngestLogPO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class SubjectMatchFileIngestLogConvertorImpl implements SubjectMatchFileIngestLogConvertor {

    @Override
    public SubjectMatchFileIngestLogPO toPO(SubjectMatchFileIngestLog ingestLog) {
        if ( ingestLog == null ) {
            return null;
        }

        SubjectMatchFileIngestLogPO subjectMatchFileIngestLogPO = new SubjectMatchFileIngestLogPO();

        subjectMatchFileIngestLogPO.setIngestId( ingestLog.getIngestId() );
        subjectMatchFileIngestLogPO.setFileId( ingestLog.getFileId() );
        if ( ingestLog.getSourceChannel() != null ) {
            subjectMatchFileIngestLogPO.setSourceChannel( ingestLog.getSourceChannel().name() );
        }
        subjectMatchFileIngestLogPO.setSourceUri( ingestLog.getSourceUri() );
        subjectMatchFileIngestLogPO.setChannelMessageId( ingestLog.getChannelMessageId() );
        subjectMatchFileIngestLogPO.setIngestStatus( ingestLog.getIngestStatus() );
        subjectMatchFileIngestLogPO.setIngestTime( ingestLog.getIngestTime() );
        subjectMatchFileIngestLogPO.setIngestMetaJson( ingestLog.getIngestMetaJson() );
        subjectMatchFileIngestLogPO.setCreatedBy( ingestLog.getCreatedBy() );
        subjectMatchFileIngestLogPO.setErrorMessage( ingestLog.getErrorMessage() );

        return subjectMatchFileIngestLogPO;
    }

    @Override
    public SubjectMatchFileIngestLog toDomain(SubjectMatchFileIngestLogPO po) {
        if ( po == null ) {
            return null;
        }

        SubjectMatchFileIngestLog.SubjectMatchFileIngestLogBuilder subjectMatchFileIngestLog = SubjectMatchFileIngestLog.builder();

        subjectMatchFileIngestLog.ingestId( po.getIngestId() );
        subjectMatchFileIngestLog.fileId( po.getFileId() );
        if ( po.getSourceChannel() != null ) {
            subjectMatchFileIngestLog.sourceChannel( Enum.valueOf( SubjectMatchFileSourceChannel.class, po.getSourceChannel() ) );
        }
        subjectMatchFileIngestLog.sourceUri( po.getSourceUri() );
        subjectMatchFileIngestLog.channelMessageId( po.getChannelMessageId() );
        subjectMatchFileIngestLog.ingestStatus( po.getIngestStatus() );
        subjectMatchFileIngestLog.ingestTime( po.getIngestTime() );
        subjectMatchFileIngestLog.ingestMetaJson( po.getIngestMetaJson() );
        subjectMatchFileIngestLog.createdBy( po.getCreatedBy() );
        subjectMatchFileIngestLog.errorMessage( po.getErrorMessage() );

        return subjectMatchFileIngestLog.build();
    }
}

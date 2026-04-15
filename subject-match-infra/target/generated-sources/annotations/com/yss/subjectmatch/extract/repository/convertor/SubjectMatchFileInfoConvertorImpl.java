package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.SubjectMatchFileInfo;
import com.yss.subjectmatch.domain.model.SubjectMatchFileSourceChannel;
import com.yss.subjectmatch.domain.model.SubjectMatchFileStatus;
import com.yss.subjectmatch.domain.model.SubjectMatchFileStorageType;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchFileInfoPO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class SubjectMatchFileInfoConvertorImpl implements SubjectMatchFileInfoConvertor {

    @Override
    public SubjectMatchFileInfoPO toPO(SubjectMatchFileInfo fileInfo) {
        if ( fileInfo == null ) {
            return null;
        }

        SubjectMatchFileInfoPO subjectMatchFileInfoPO = new SubjectMatchFileInfoPO();

        subjectMatchFileInfoPO.setFileId( fileInfo.getFileId() );
        subjectMatchFileInfoPO.setFileNameOriginal( fileInfo.getFileNameOriginal() );
        subjectMatchFileInfoPO.setFileNameNormalized( fileInfo.getFileNameNormalized() );
        subjectMatchFileInfoPO.setFileExtension( fileInfo.getFileExtension() );
        subjectMatchFileInfoPO.setMimeType( fileInfo.getMimeType() );
        subjectMatchFileInfoPO.setFileSizeBytes( fileInfo.getFileSizeBytes() );
        subjectMatchFileInfoPO.setFileFingerprint( fileInfo.getFileFingerprint() );
        if ( fileInfo.getSourceChannel() != null ) {
            subjectMatchFileInfoPO.setSourceChannel( fileInfo.getSourceChannel().name() );
        }
        subjectMatchFileInfoPO.setSourceUri( fileInfo.getSourceUri() );
        if ( fileInfo.getStorageType() != null ) {
            subjectMatchFileInfoPO.setStorageType( fileInfo.getStorageType().name() );
        }
        subjectMatchFileInfoPO.setStorageUri( fileInfo.getStorageUri() );
        subjectMatchFileInfoPO.setFileFormat( fileInfo.getFileFormat() );
        if ( fileInfo.getFileStatus() != null ) {
            subjectMatchFileInfoPO.setFileStatus( fileInfo.getFileStatus().name() );
        }
        subjectMatchFileInfoPO.setCreatedBy( fileInfo.getCreatedBy() );
        subjectMatchFileInfoPO.setReceivedAt( fileInfo.getReceivedAt() );
        subjectMatchFileInfoPO.setStoredAt( fileInfo.getStoredAt() );
        subjectMatchFileInfoPO.setLastProcessedAt( fileInfo.getLastProcessedAt() );
        subjectMatchFileInfoPO.setLastTaskId( fileInfo.getLastTaskId() );
        subjectMatchFileInfoPO.setErrorMessage( fileInfo.getErrorMessage() );
        subjectMatchFileInfoPO.setSourceMetaJson( fileInfo.getSourceMetaJson() );
        subjectMatchFileInfoPO.setStorageMetaJson( fileInfo.getStorageMetaJson() );
        subjectMatchFileInfoPO.setRemark( fileInfo.getRemark() );

        return subjectMatchFileInfoPO;
    }

    @Override
    public SubjectMatchFileInfo toDomain(SubjectMatchFileInfoPO po) {
        if ( po == null ) {
            return null;
        }

        SubjectMatchFileInfo.SubjectMatchFileInfoBuilder subjectMatchFileInfo = SubjectMatchFileInfo.builder();

        subjectMatchFileInfo.fileId( po.getFileId() );
        subjectMatchFileInfo.fileNameOriginal( po.getFileNameOriginal() );
        subjectMatchFileInfo.fileNameNormalized( po.getFileNameNormalized() );
        subjectMatchFileInfo.fileExtension( po.getFileExtension() );
        subjectMatchFileInfo.mimeType( po.getMimeType() );
        subjectMatchFileInfo.fileSizeBytes( po.getFileSizeBytes() );
        subjectMatchFileInfo.fileFingerprint( po.getFileFingerprint() );
        if ( po.getSourceChannel() != null ) {
            subjectMatchFileInfo.sourceChannel( Enum.valueOf( SubjectMatchFileSourceChannel.class, po.getSourceChannel() ) );
        }
        subjectMatchFileInfo.sourceUri( po.getSourceUri() );
        if ( po.getStorageType() != null ) {
            subjectMatchFileInfo.storageType( Enum.valueOf( SubjectMatchFileStorageType.class, po.getStorageType() ) );
        }
        subjectMatchFileInfo.storageUri( po.getStorageUri() );
        subjectMatchFileInfo.fileFormat( po.getFileFormat() );
        if ( po.getFileStatus() != null ) {
            subjectMatchFileInfo.fileStatus( Enum.valueOf( SubjectMatchFileStatus.class, po.getFileStatus() ) );
        }
        subjectMatchFileInfo.createdBy( po.getCreatedBy() );
        subjectMatchFileInfo.receivedAt( po.getReceivedAt() );
        subjectMatchFileInfo.storedAt( po.getStoredAt() );
        subjectMatchFileInfo.lastProcessedAt( po.getLastProcessedAt() );
        subjectMatchFileInfo.lastTaskId( po.getLastTaskId() );
        subjectMatchFileInfo.errorMessage( po.getErrorMessage() );
        subjectMatchFileInfo.sourceMetaJson( po.getSourceMetaJson() );
        subjectMatchFileInfo.storageMetaJson( po.getStorageMetaJson() );
        subjectMatchFileInfo.remark( po.getRemark() );

        return subjectMatchFileInfo.build();
    }
}

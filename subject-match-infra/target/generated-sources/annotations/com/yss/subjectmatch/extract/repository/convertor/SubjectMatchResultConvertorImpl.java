package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.ConfidenceLevel;
import com.yss.subjectmatch.domain.model.SubjectMatchResult;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchResultPO;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class SubjectMatchResultConvertorImpl implements SubjectMatchResultConvertor {

    @Autowired
    private JsonStringMapper jsonStringMapper;

    @Override
    public SubjectMatchResultPO toPO(Long taskId, Long fileId, SubjectMatchResult result) {
        if ( taskId == null && fileId == null && result == null ) {
            return null;
        }

        SubjectMatchResultPO subjectMatchResultPO = new SubjectMatchResultPO();

        if ( result != null ) {
            subjectMatchResultPO.setTopCandidatesJson( jsonStringMapper.toJson( result.getTopCandidates() ) );
            subjectMatchResultPO.setExternalSubjectCode( jsonStringMapper.toJson( result.getExternalSubjectCode() ) );
            subjectMatchResultPO.setExternalSubjectName( jsonStringMapper.toJson( result.getExternalSubjectName() ) );
            subjectMatchResultPO.setExternalLevel( result.getExternalLevel() );
            subjectMatchResultPO.setExternalIsLeaf( result.getExternalIsLeaf() );
            subjectMatchResultPO.setAnchorSubjectCode( jsonStringMapper.toJson( result.getAnchorSubjectCode() ) );
            subjectMatchResultPO.setAnchorSubjectName( jsonStringMapper.toJson( result.getAnchorSubjectName() ) );
            subjectMatchResultPO.setAnchorLevel( result.getAnchorLevel() );
            subjectMatchResultPO.setAnchorPathText( jsonStringMapper.toJson( result.getAnchorPathText() ) );
            subjectMatchResultPO.setAnchorReason( jsonStringMapper.toJson( result.getAnchorReason() ) );
            subjectMatchResultPO.setMatchedStandardCode( jsonStringMapper.toJson( result.getMatchedStandardCode() ) );
            subjectMatchResultPO.setMatchedStandardName( jsonStringMapper.toJson( result.getMatchedStandardName() ) );
            subjectMatchResultPO.setScore( result.getScore() );
            subjectMatchResultPO.setScoreName( result.getScoreName() );
            subjectMatchResultPO.setScorePath( result.getScorePath() );
            subjectMatchResultPO.setScoreKeyword( result.getScoreKeyword() );
            subjectMatchResultPO.setScoreCode( result.getScoreCode() );
            subjectMatchResultPO.setScoreHistory( result.getScoreHistory() );
            subjectMatchResultPO.setScoreEmbedding( result.getScoreEmbedding() );
            subjectMatchResultPO.setConfidenceLevel( jsonStringMapper.toJson( result.getConfidenceLevel() ) );
            subjectMatchResultPO.setNeedsReview( result.getNeedsReview() );
            subjectMatchResultPO.setMatchReason( jsonStringMapper.toJson( result.getMatchReason() ) );
            subjectMatchResultPO.setCandidateCount( result.getCandidateCount() );
        }
        subjectMatchResultPO.setTaskId( taskId );
        subjectMatchResultPO.setFileId( fileId );

        return subjectMatchResultPO;
    }

    @Override
    public SubjectMatchResult toDomain(SubjectMatchResultPO po) {
        if ( po == null ) {
            return null;
        }

        SubjectMatchResult.SubjectMatchResultBuilder subjectMatchResult = SubjectMatchResult.builder();

        subjectMatchResult.topCandidates( jsonStringMapper.toListMatchCandidate( po.getTopCandidatesJson() ) );
        subjectMatchResult.externalSubjectCode( jsonStringMapper.toJson( po.getExternalSubjectCode() ) );
        subjectMatchResult.externalSubjectName( jsonStringMapper.toJson( po.getExternalSubjectName() ) );
        subjectMatchResult.externalLevel( po.getExternalLevel() );
        subjectMatchResult.externalIsLeaf( po.getExternalIsLeaf() );
        subjectMatchResult.anchorSubjectCode( jsonStringMapper.toJson( po.getAnchorSubjectCode() ) );
        subjectMatchResult.anchorSubjectName( jsonStringMapper.toJson( po.getAnchorSubjectName() ) );
        subjectMatchResult.anchorLevel( po.getAnchorLevel() );
        subjectMatchResult.anchorPathText( jsonStringMapper.toJson( po.getAnchorPathText() ) );
        subjectMatchResult.anchorReason( jsonStringMapper.toJson( po.getAnchorReason() ) );
        subjectMatchResult.matchedStandardCode( jsonStringMapper.toJson( po.getMatchedStandardCode() ) );
        subjectMatchResult.matchedStandardName( jsonStringMapper.toJson( po.getMatchedStandardName() ) );
        subjectMatchResult.score( po.getScore() );
        subjectMatchResult.scoreName( po.getScoreName() );
        subjectMatchResult.scorePath( po.getScorePath() );
        subjectMatchResult.scoreKeyword( po.getScoreKeyword() );
        subjectMatchResult.scoreCode( po.getScoreCode() );
        subjectMatchResult.scoreHistory( po.getScoreHistory() );
        subjectMatchResult.scoreEmbedding( po.getScoreEmbedding() );
        if ( po.getConfidenceLevel() != null ) {
            subjectMatchResult.confidenceLevel( Enum.valueOf( ConfidenceLevel.class, po.getConfidenceLevel() ) );
        }
        subjectMatchResult.needsReview( po.getNeedsReview() );
        subjectMatchResult.matchReason( jsonStringMapper.toJson( po.getMatchReason() ) );
        subjectMatchResult.candidateCount( po.getCandidateCount() );

        return subjectMatchResult.build();
    }
}

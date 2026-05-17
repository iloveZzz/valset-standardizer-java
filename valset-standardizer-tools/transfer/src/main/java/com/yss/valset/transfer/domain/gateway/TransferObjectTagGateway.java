package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferObjectTag;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件对象标签结果网关。
 */
public interface TransferObjectTagGateway {

    List<TransferObjectTag> listByTransferId(String transferId);

    List<TransferObjectTag> listByTransferIds(List<String> transferIds);

    List<TransferObjectTag> saveAll(List<TransferObjectTag> tags);

    void deleteByTransferId(String transferId);

    List<TagSummary> summarizeTags(LocalDateTime startInclusive, LocalDateTime endExclusive);

    final class TagSummary {
        private final String tagCode;
        private final String tagName;
        private final long tagCount;

        public TagSummary(String tagCode, String tagName, long tagCount) {
            this.tagCode = tagCode;
            this.tagName = tagName;
            this.tagCount = tagCount;
        }

        public String tagCode() {
            return tagCode;
        }

        public String tagName() {
            return tagName;
        }

        public long tagCount() {
            return tagCount;
        }

        public String getTagCode() {
            return tagCode;
        }

        public String getTagName() {
            return tagName;
        }

        public long getTagCount() {
            return tagCount;
        }
    }
}

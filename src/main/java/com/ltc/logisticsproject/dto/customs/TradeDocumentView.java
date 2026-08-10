package com.ltc.logisticsproject.dto.customs;

import com.ltc.logisticsproject.entity.DocumentStatus;
import com.ltc.logisticsproject.entity.DocumentType;
import com.ltc.logisticsproject.entity.TradeDocument;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Müştəriyə göstərilən sənəd görünüşü — uploadedByName kimi daxili
// məlumatları gizlədir, yalnız sənədin növü/statusu/faylını açır.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TradeDocumentView {
    Long id;
    DocumentType type;
    String fileUrl;
    DocumentStatus status;
    String createdAt;

    public static TradeDocumentView from(TradeDocument doc) {
        return TradeDocumentView.builder()
                .id(doc.getId())
                .type(doc.getType())
                .fileUrl(doc.getFileUrl())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
                .build();
    }
}

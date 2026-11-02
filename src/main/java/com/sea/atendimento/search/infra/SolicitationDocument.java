package com.sea.atendimento.search.infra;

import com.sea.atendimento.solicitation.domain.Solicitation;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Document(indexName = "solicitations")
public record SolicitationDocument(

        @Id
        Long id,

        @Field(type = FieldType.Long)
        Long clientId,

        @Field(type = FieldType.Keyword)
        String status,

        @Field(type = FieldType.Keyword)
        String serviceType,

        @Field(type = FieldType.Keyword)
        String priority,

        @Field(type = FieldType.Text, analyzer = "brazilian")
        String title,

        @Field(type = FieldType.Text, analyzer = "brazilian")
        String description,

        @Field(type = FieldType.Keyword)
        String uf,

        @Field(type = FieldType.Keyword)
        String city,

        @Field(type = FieldType.Double)
        BigDecimal estimatedValue,

        @Field(type = FieldType.Date, format = DateFormat.date)
        LocalDate preferredDate,

        @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
        Instant createdAt,

        @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
        Instant submittedAt
) {
    public static SolicitationDocument from(Solicitation s) {
        return new SolicitationDocument(
                s.getId(),
                s.getClient().getId(),
                s.getStatus().name(),
                s.getServiceType().name(),
                s.getPriority() == null ? null : s.getPriority().name(),
                s.getTitle(),
                s.getDescription(),
                s.getUf() == null ? null : s.getUf().name(),
                s.getAddress() == null ? null : s.getAddress().getCity(),
                s.getEstimatedValue(),
                s.getPreferredDate(),
                s.getCreatedAt(),
                s.getSubmittedAt()
        );
    }
}
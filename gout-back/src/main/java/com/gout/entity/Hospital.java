package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "hospitals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hospital extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "hira_code", unique = true)
    private String hiraCode;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    private Double latitude;

    private Double longitude;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "departments", columnDefinition = "text[]")
    private List<String> departments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operating_hours", columnDefinition = "jsonb")
    private String operatingHours;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private Hospital(String hiraCode,
                     String name,
                     String address,
                     String phone,
                     Double latitude,
                     Double longitude,
                     List<String> departments,
                     String operatingHours,
                     Boolean isActive) {
        this.hiraCode = hiraCode;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.departments = departments;
        this.operatingHours = operatingHours;
        this.isActive = isActive != null ? isActive : Boolean.TRUE;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}

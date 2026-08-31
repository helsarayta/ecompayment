package com.heydie.ecompayment.entity.baseentity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class BaseEntity implements Serializable {
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime modifiedDate;

    @Column(nullable = false, updatable = false)
    private String createdBy;

    @Column(nullable = false)
    private String modifiedBy;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        createdBy = getCurrentUser();
        if(createdBy != null) {
            modifiedBy = createdBy;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
        modifiedBy = getCurrentUser();
    }

    private String getCurrentUser() {
        return "System";
    }
}


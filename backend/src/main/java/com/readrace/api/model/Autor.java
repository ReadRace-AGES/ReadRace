package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "autor")
public class Autor {

    @Id private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Autor outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

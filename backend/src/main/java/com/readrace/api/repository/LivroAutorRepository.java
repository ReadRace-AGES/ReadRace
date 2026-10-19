package com.readrace.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.LivroAutor;
import com.readrace.api.model.LivroAutorId;

public interface LivroAutorRepository extends JpaRepository<LivroAutor, LivroAutorId> {}

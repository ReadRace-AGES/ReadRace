package com.readrace.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Exemplo;

/**
 * Só a interface — o Spring Data gera a implementação sozinho.
 *
 * <p>O JpaRepository já entrega findAll, findById, save, delete e companhia. Consulta específica:
 * declare o método seguindo a convenção de nomes (findByNome, existsByNome...) que ele monta a
 * query.
 */
public interface ExemploRepository extends JpaRepository<Exemplo, Long> {}

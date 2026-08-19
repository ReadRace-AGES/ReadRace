package com.readrace.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidade = espelho da tabela criada na migration.
 *
 * <p>O Hibernate está em ddl-auto=validate: ele NÃO cria nem altera tabela, só confere se a
 * entidade bate com o banco. Mexeu aqui, tem que ter uma migration correspondente no mesmo PR,
 * senão a aplicação não sobe.
 */
@Entity
@Table(name = "exemplo")
public class Exemplo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    /** Exigido pelo JPA. Não use no código da aplicação. */
    protected Exemplo() {}

    public Exemplo(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    /**
     * Alteração de estado passa por um método com nome de negócio, em vez de setters espalhados.
     * Assim existe um único ponto de entrada para mudar a entidade — e uma casa óbvia para a
     * primeira regra que aparecer.
     */
    public void atualizar(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }
}

package com.whtngml.subscription_cancel_guide.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length =30)
    private String name;

    @Column(nullable = false,unique = true, length =30)
    private String slug;

    public Category(String name, String slug){
        this.name = name;
        this.slug = slug;
    }

    // 주소(slug)는 바꾸지 않고 이름만 고친다
    public void rename(String name){
        this.name = name;
    }
}

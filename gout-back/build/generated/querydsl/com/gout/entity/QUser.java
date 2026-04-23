package com.gout.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -1573304836L;

    public static final QUser user = new QUser("user");

    public final com.gout.global.entity.QBaseEntity _super = new com.gout.global.entity.QBaseEntity(this);

    public final NumberPath<Integer> birthYear = createNumber("birthYear", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> consentSensitiveAt = createDateTime("consentSensitiveAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final EnumPath<User.Gender> gender = createEnum("gender", User.Gender.class);

    public final StringPath id = createString("id");

    public final StringPath kakaoId = createString("kakaoId");

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final EnumPath<User.Role> role = createEnum("role", User.Role.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}


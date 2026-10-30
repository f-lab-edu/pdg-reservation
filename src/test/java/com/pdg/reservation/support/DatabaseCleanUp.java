package com.pdg.reservation.support;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseCleanUp implements InitializingBean {

    @PersistenceContext
    private EntityManager entityManager;

    private final List<String> tableNames = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        entityManager.getMetamodel().getEntities().stream()
                .filter(entity -> entity.getJavaType().getAnnotation(Entity.class) != null)
                .map(entity -> {
                    Table table = entity.getJavaType().getAnnotation(Table.class);
                    if (table == null) {
                        throw new IllegalStateException(entity.getJavaType().getName() + "에 @Table이 없어 테이블 이름을 알 수 없습니다.");
                    }
                    return table.name();
                })
                .forEach(tableNames::add);
    }

    // FOREIGN_KEY_CHECKS는 연결 단위 설정이라 끄기·TRUNCATE·켜기가 같은 연결에서 실행되도록 트랜잭션으로 묶는다.
    @Transactional
    public void truncateAllTables() {
        entityManager.clear();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        for (String table : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE `" + table + "`").executeUpdate();
        }

        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }
}

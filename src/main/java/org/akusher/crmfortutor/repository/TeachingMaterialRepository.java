package org.akusher.crmfortutor.repository;

import org.akusher.crmfortutor.entity.TeachingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingMaterialRepository extends JpaRepository<TeachingMaterial, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<TeachingMaterial> {

    Optional<TeachingMaterial> findByIdAndTutorId(Long id, Long tutorId);

    default List<TeachingMaterial> findByTutorIdAndFilters(Long tutorId, String category, String search) {
        org.springframework.data.jpa.domain.Specification<TeachingMaterial> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("tutor").get("id"), tutorId));

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("originalFileName")), pattern)
                ));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return findAll(spec);
    }

    @Query("""
        SELECT DISTINCT m.category FROM TeachingMaterial m
        WHERE m.tutor.id = :tutorId
          AND m.category IS NOT NULL
          AND TRIM(m.category) <> ''
        ORDER BY m.category ASC
    """)
    List<String> findDistinctCategoriesByTutorId(@Param("tutorId") Long tutorId);
}

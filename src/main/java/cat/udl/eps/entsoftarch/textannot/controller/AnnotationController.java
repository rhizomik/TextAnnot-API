package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.Annotation;
import cat.udl.eps.entsoftarch.textannot.domain.QAnnotation;
import cat.udl.eps.entsoftarch.textannot.domain.QTag;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.service.ControllerUtilities;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

@BasePathAwareController
public class AnnotationController {

    @Autowired
    ControllerUtilities controllerUtilities;

    @Autowired
    EntityManager entityManager;

    JPAQueryFactory queryFactory;

    @PostConstruct
    public void init() {
        queryFactory = new JPAQueryFactory(entityManager);
    }

    @RequestMapping("/annotations/search/findBySampleAndTags")
    public @ResponseBody
    Resources<PersistentEntityResource> findAnnotationBySampleAndTags(@RequestParam("sample") Integer sampleId,
                                                                       @RequestParam("tags")List<String> tagIds,
                                                                       PersistentEntityResourceAssembler assembler) {
        List<Tag> tags = controllerUtilities.getTagsFromIds(tagIds);
        List<Annotation> annotations = queryFactory.selectFrom(QAnnotation.annotation)
                .innerJoin(QAnnotation.annotation.tag, QTag.tag)
                .where(QAnnotation.annotation.sample.id.eq(sampleId)
                        .and(controllerUtilities.getTagQuery(tags)))
                .groupBy(QAnnotation.annotation.start, QAnnotation.annotation.end)
                .having(QAnnotation.annotation.countDistinct().goe(Long.valueOf(tags.size()))).fetch();

        return new Resources<>(annotations.stream().map(assembler::toResource).collect(Collectors.toList()));
    }
}

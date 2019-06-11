package cat.udl.eps.entsoftarch.textannot.service;

import cat.udl.eps.entsoftarch.textannot.controller.TagHierarchyController;
import cat.udl.eps.entsoftarch.textannot.domain.QTag;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.domain.TagHierarchy;
import cat.udl.eps.entsoftarch.textannot.repository.TagHierarchyRepository;
import cat.udl.eps.entsoftarch.textannot.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagHierarchyPrecalcService {

    @Autowired
    TagRepository tagRepository;

    @Autowired
    TagHierarchyRepository tagHierarchyRepository;

    public void recalculateTagHierarchyTree(TagHierarchy tagHierarchy) throws JsonProcessingException {
        List<Tag> roots = tagRepository.findByTagHierarchyAndParentIsNull(tagHierarchy);
        TagHierarchyJson tagHierarchyJson = retrieveTagHierarchyTree(tagHierarchy, roots);
        ObjectMapper mapper = new ObjectMapper();
        tagHierarchy.setPrecalculatedTagTree(mapper.writeValueAsString(tagHierarchyJson));
        tagHierarchyRepository.save(tagHierarchy);
    }

    private TagHierarchyJson retrieveTagHierarchyTree(TagHierarchy tagHierarchy, List<Tag> roots) {
        TagHierarchyJson tagHierarchyJson = new TagHierarchyJson(tagHierarchy);
        tagHierarchyJson.setRoots(retrieveTagsTree(roots));
        return tagHierarchyJson;
    }

    private List<TagJson> retrieveTagsTree(List<Tag> roots) {
        List<TagJson> tagJsons = roots.stream().map(TagJson::new).collect(Collectors.toList());
        tagJsons.forEach(this::setChildren);
        return tagJsons;
    }

    private void setChildren(TagJson root) {
        List<TagJson> children =
                tagRepository.findByParentId(root.getId())
                        .stream()
                        .map(TagJson::new)
                        .collect(Collectors.toList());

        root.getChildren().addAll(children);

        children.forEach(this::setChildren);
    }

    @Data
    public static class TagHierarchyJson {
        private String name;
        private Integer id;
        private List<TagJson> roots;

        TagHierarchyJson() {}

        TagHierarchyJson(TagHierarchy tagHierarchy) {
            this.id = tagHierarchy.getId();
            this.name = tagHierarchy.getName();
        }
    }

    @Data
    public static class TagJson {
        private Integer id;
        private String name;
        private List<TagJson> children;

        TagJson() {}

        TagJson(Tag tag) {
            this.id = tag.getId();
            this.name = tag.getName();
            children = new ArrayList<>();
        }
    }
}

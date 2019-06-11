package cat.udl.eps.entsoftarch.textannot.service;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
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
    ProjectRepository projectRepository;

    public void recalculateTagHierarchyTree(Project project) throws JsonProcessingException {
        List<Tag> roots = tagRepository.findByProjectAndParentIsNull(project);
        TagHierarchyJson tagHierarchyJson = retrieveTagHierarchyTree(project, roots);
        ObjectMapper mapper = new ObjectMapper();
        project.setPrecalculatedTagTree(mapper.writeValueAsString(tagHierarchyJson));
        projectRepository.save(project);
    }

    private TagHierarchyJson retrieveTagHierarchyTree(Project project, List<Tag> roots) {
        TagHierarchyJson tagHierarchyJson = new TagHierarchyJson(project);
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

        TagHierarchyJson(Project project) {
            this.id = project.getId();
            this.name = project.getName();
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

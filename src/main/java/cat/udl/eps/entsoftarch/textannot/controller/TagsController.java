package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.exception.TagHierarchyDuplicateException;
import cat.udl.eps.entsoftarch.textannot.exception.TagHierarchyValidationException;
import cat.udl.eps.entsoftarch.textannot.exception.TagTreeException;
import cat.udl.eps.entsoftarch.textannot.repository.AnnotationRepository;
import cat.udl.eps.entsoftarch.textannot.repository.ProjectRepository;
import cat.udl.eps.entsoftarch.textannot.repository.TagRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import cat.udl.eps.entsoftarch.textannot.service.TagHierarchyPrecalcService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;

@BasePathAwareController
public class TagsController {

    private ProjectRepository projectRepository;
    private TagRepository tagRepository;
    private TagHierarchyPrecalcService tagHierarchyPrecalcService;
    private AnnotationRepository annotationRepository;

    public TagsController(ProjectRepository projectRepository, TagRepository tagRepository,
                          TagHierarchyPrecalcService tagHierarchyPrecalcService,
                          AnnotationRepository annotationRepository) {
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
        this.tagHierarchyPrecalcService = tagHierarchyPrecalcService;
        this.annotationRepository = annotationRepository;
    }

    @PostMapping(value = "projects/{projectId}/tags", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public void quickTagHierarchyCreate(
            @PathVariable("projectId") Integer projectId,
            @RequestBody TagHierarchyPrecalcService.TagHierarchyJson body,
            PersistentEntityResourceAssembler resourceAssembler) throws JsonProcessingException {

        Optional<Project> project = projectRepository.findById(projectId);
        if (!project.isPresent() || project.get().getPrecalculatedTagTree() != null)
            throw new TagHierarchyValidationException();

        List<Tag> treeHierarchy = new ArrayList<>();


        Optional.ofNullable(body.getRoots())
            .orElseThrow(TagHierarchyValidationException::new)
            .forEach(root -> createTag(root, null, project.get(), treeHierarchy));

        treeHierarchy.forEach(tagRepository::save);

        tagHierarchyPrecalcService.recalculateTagHierarchyTree(project.get());
    }

    private void createTag(TagHierarchyPrecalcService.TagJson tagJson, Tag parent, Project project, List<Tag> treeHierarchy) {
        if (isNullOrEmpty(tagJson.getName()))
            throw new TagHierarchyValidationException();

        Tag tag = new Tag(tagJson.getName());
        tag.setParent(parent);
        tag.setProject(project);

        if(treeHierarchy.stream().anyMatch(t -> t.getName().equals(tag.getName())))
            throw new TagTreeException();

        treeHierarchy.add(tag);

        Optional.ofNullable(tagJson.getChildren())
            .ifPresent(children ->
                    children.forEach(child -> createTag(child, tag, project, treeHierarchy)));
    }

    private boolean isNullOrEmpty(String name) {
        return name == null || name.isEmpty();
    }

    @GetMapping(value = "/projects/{id}/tags", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public @ResponseBody
    JsonNode tagHierarchyDetail(@PathVariable("id") Integer id) throws IOException {

        Project project = projectRepository.findById(id)
                .orElseThrow(ResourceNotFoundException::new);

        if (project.getPrecalculatedTagTree() == null || project.getPrecalculatedTagTree().isEmpty()){
            tagHierarchyPrecalcService.recalculateTagHierarchyTree(project);
            projectRepository.save(project);
        }
        return new ObjectMapper().readTree(project.getPrecalculatedTagTree());
    }

    @PostMapping(value = "/projects/{id}/tags", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public void quickTagHierarchyCreateCSV(
            @PathVariable("id") Integer id,
        ServletServerHttpRequest request,
        PersistentEntityResourceAssembler resourceAssembler) throws IOException {

        Optional<Project> project = projectRepository.findById(id);
        if (!project.isPresent() || project.get().getPrecalculatedTagTree() != null)
            throw new TagHierarchyValidationException();

        readCSVAndSaveTags(request.getBody(), project.get());
        tagHierarchyPrecalcService.recalculateTagHierarchyTree(project.get());
    }

    private void readCSVAndSaveTags(InputStream csvStream, Project project) throws IOException {
        HashMap<String, Tag> processedTags = new HashMap<>();
        CSVFormat excelCSV = CSVFormat.newFormat(';').withRecordSeparator('\n').withFirstRecordAsHeader();
        CSVParser parser = CSVParser.parse(csvStream, StandardCharsets.UTF_8, excelCSV);
        parser.getRecords().forEach(record -> {
            Tag parent = null;
            //TODO: currently ignoring last column with tagging examples
            for(int i = 0; i < record.size()-1; i++) {
                String tagName = record.get(i);
                if (isNullOrEmpty(tagName))
                    continue;
                if (!processedTags.containsKey(tagName)) {
                    Tag tag = new Tag(tagName);
                    tag.setParent(parent);
                    tag.setProject(project);
                    processedTags.put(tagName, tagRepository.save(tag));
                    parent = tag;
                }
                else {
                    parent = processedTags.get(tagName);
                }
            }
        });
    }

    @PostMapping(value = "/projects/{id}/tags", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public void quickTagHierarchyCreateFileCSV(
            @PathVariable("id") Integer projectId,
            @RequestParam("file") MultipartFile file) throws IOException {
        Optional<Project> project = projectRepository.findById(projectId);
        if (!project.isPresent())
            throw new TagHierarchyValidationException();

        readCSVAndSaveTags(file.getInputStream(), project.get());
        tagHierarchyPrecalcService.recalculateTagHierarchyTree(project.get());
    }

    @DeleteMapping(value = "/projects/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteTagsTree(
            @PathVariable("id") Integer projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        if (!project.isPresent())
            throw new TagHierarchyValidationException();

        this.annotationRepository.deleteByTagProject(project.get());
        this.tagRepository.deleteByProject(project.get());
        project.get().setPrecalculatedTagTree("");
        this.projectRepository.save(project.get());
    }
}

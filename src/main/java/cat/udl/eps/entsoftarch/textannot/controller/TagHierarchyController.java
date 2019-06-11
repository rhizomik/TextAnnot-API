package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.Project;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.exception.TagHierarchyDuplicateException;
import cat.udl.eps.entsoftarch.textannot.exception.TagHierarchyValidationException;
import cat.udl.eps.entsoftarch.textannot.exception.TagTreeException;
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
public class TagHierarchyController {

    private ProjectRepository projectRepository;
    private TagRepository tagRepository;
    private TagHierarchyPrecalcService tagHierarchyPrecalcService;

    public TagHierarchyController(ProjectRepository projectRepository, TagRepository tagRepository,
                                  TagHierarchyPrecalcService tagHierarchyPrecalcService) {
        this.projectRepository = projectRepository;
        this.tagRepository = tagRepository;
        this.tagHierarchyPrecalcService = tagHierarchyPrecalcService;
    }

    @PostMapping(value = "/quickTagHierarchyCreate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public PersistentEntityResource quickTagHierarchyCreate(
            @RequestBody TagHierarchyPrecalcService.TagHierarchyJson body,
            PersistentEntityResourceAssembler resourceAssembler) {

        if (isNullOrEmpty(body.getName()))
            throw new TagHierarchyValidationException();


        if (projectRepository.findByName(body.getName()) != null)
            throw new TagHierarchyDuplicateException();

        List<Tag> treeHierarchy = new ArrayList<>();

        Project project = new Project();
        project.setName(body.getName());

        Optional.ofNullable(body.getRoots())
            .orElseThrow(TagHierarchyValidationException::new)
            .forEach(root -> createTag(root, null, project, treeHierarchy));

        projectRepository.save(project);
        treeHierarchy.forEach(tagRepository::save);

        return resourceAssembler.toResource(project);
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

    @GetMapping(value = "/tagHierarchies/{id}/tags", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public @ResponseBody
    JsonNode tagHierarchyDetail(@PathVariable("id") Integer id) throws IOException {

        Project project = projectRepository.findById(id)
                .orElseThrow(ResourceNotFoundException::new);

        if (project.getPrecalculatedTagTree() == null){
            tagHierarchyPrecalcService.recalculateTagHierarchyTree(project);
            projectRepository.save(project);
        }
        return new ObjectMapper().readTree(project.getPrecalculatedTagTree());
    }

    @PostMapping(value = "/quickTagHierarchyCreate", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public PersistentEntityResource quickTagHierarchyCreateCSV(
        @RequestParam("project") String tagHierarchyName,
        ServletServerHttpRequest request,
        PersistentEntityResourceAssembler resourceAssembler) throws IOException {

        Project project = createTagHierarchy(tagHierarchyName);

        readCSVAndSaveTags(request.getBody(), project);
        return resourceAssembler.toResource(project);
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
                    processedTags.put(tagName, tag);
                    parent = tag;
                }
                else {
                    parent = processedTags.get(tagName);
                }
            }
        });
        tagRepository.saveAll(processedTags.values());
    }

    private Project createTagHierarchy(@RequestParam("project") String tagHierarchyName) {
        if (isNullOrEmpty(tagHierarchyName))
            throw new TagHierarchyValidationException();

        if (projectRepository.findByName(tagHierarchyName) != null)
            throw new TagHierarchyDuplicateException();

        Project project = new Project();
        project.setName(tagHierarchyName);
        return projectRepository.save(project);
    }

    @PostMapping(value = "/quickTagHierarchyCreate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public PersistentEntityResource quickTagHierarchyCreateFileCSV(
            @RequestParam("tagHierarchyName") String tagHierarchyName,
            @RequestParam("file") MultipartFile file,
            PersistentEntityResourceAssembler resourceAssembler) throws IOException {
        Project project = createTagHierarchy(tagHierarchyName);
        readCSVAndSaveTags(file.getInputStream(), project);
        return resourceAssembler.toResource(project);
    }
}

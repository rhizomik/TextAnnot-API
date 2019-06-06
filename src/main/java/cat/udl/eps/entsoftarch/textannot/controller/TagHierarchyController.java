package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.domain.TagHierarchy;
import cat.udl.eps.entsoftarch.textannot.exception.TagHierarchyDuplicateException;
import cat.udl.eps.entsoftarch.textannot.exception.TagHierarchyValidationException;
import cat.udl.eps.entsoftarch.textannot.exception.TagTreeException;
import cat.udl.eps.entsoftarch.textannot.repository.TagHierarchyRepository;
import cat.udl.eps.entsoftarch.textannot.repository.TagRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cat.udl.eps.entsoftarch.textannot.service.TagHierarchyPrecalcService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONWrappedObject;
import lombok.Data;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.transaction.Transactional;

@BasePathAwareController
public class TagHierarchyController {

    private TagHierarchyRepository tagHierarchyRepository;
    private TagRepository tagRepository;
    private TagHierarchyPrecalcService tagHierarchyPrecalcService;

    public TagHierarchyController(TagHierarchyRepository tagHierarchyRepository, TagRepository tagRepository,
                                  TagHierarchyPrecalcService tagHierarchyPrecalcService) {
        this.tagHierarchyRepository = tagHierarchyRepository;
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


        if (tagHierarchyRepository.findByName(body.getName()).isPresent())
            throw new TagHierarchyDuplicateException();

        List<Tag> treeHierarchy = new ArrayList<>();

        TagHierarchy tagHierarchy = new TagHierarchy();
        tagHierarchy.setName(body.getName());

        Optional.ofNullable(body.getRoots())
            .orElseThrow(TagHierarchyValidationException::new)
            .forEach(root -> createTag(root, null, tagHierarchy, treeHierarchy));

        tagHierarchyRepository.save(tagHierarchy);
        treeHierarchy.forEach(tagRepository::save);

        return resourceAssembler.toResource(tagHierarchy);
    }

    private void createTag(TagHierarchyPrecalcService.TagJson tagJson, Tag parent, TagHierarchy tagHierarchy, List<Tag> treeHierarchy) {
        if (isNullOrEmpty(tagJson.getName()))
            throw new TagHierarchyValidationException();

        Tag tag = new Tag(tagJson.getName());
        tag.setParent(parent);
        tag.setTagHierarchy(tagHierarchy);

        if(treeHierarchy.stream().anyMatch(t -> t.getName().equals(tag.getName())))
            throw new TagTreeException();

        treeHierarchy.add(tag);

        Optional.ofNullable(tagJson.getChildren())
            .ifPresent(children ->
                    children.forEach(child -> createTag(child, tag, tagHierarchy, treeHierarchy)));
    }

    private boolean isNullOrEmpty(String name) {
        return name == null || name.isEmpty();
    }

    @GetMapping(value = "/tagHierarchies/{id}/tags", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public @ResponseBody
    JsonNode tagHierarchyDetail(@PathVariable("id") Integer id) throws IOException {

        TagHierarchy tagHierarchy = tagHierarchyRepository.findById(id)
                .orElseThrow(ResourceNotFoundException::new);

        if (tagHierarchy.getPrecalculatedTagTree() == null){
            tagHierarchyPrecalcService.recalculateTagHierarchyTree(tagHierarchy);
            tagHierarchyRepository.save(tagHierarchy);
        }
        return new ObjectMapper().readTree(tagHierarchy.getPrecalculatedTagTree());
    }

    @PostMapping(value = "/quickTagHierarchyCreate", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public PersistentEntityResource quickTagHierarchyCreateCSV(
        @RequestParam("tagHierarchy") String tagHierarchyName,
        ServletServerHttpRequest request,
        PersistentEntityResourceAssembler resourceAssembler) throws IOException {

        TagHierarchy tagHierarchy = createTagHierarchy(tagHierarchyName);

        readCSVAndSaveTags(request.getBody(), tagHierarchy);
        return resourceAssembler.toResource(tagHierarchy);
    }

    private void readCSVAndSaveTags(InputStream csvStream, TagHierarchy tagHierarchy) throws IOException {
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
                    tag.setTagHierarchy(tagHierarchy);
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

    private TagHierarchy createTagHierarchy(@RequestParam("tagHierarchy") String tagHierarchyName) {
        if (isNullOrEmpty(tagHierarchyName))
            throw new TagHierarchyValidationException();

        if (tagHierarchyRepository.findByName(tagHierarchyName).isPresent())
            throw new TagHierarchyDuplicateException();

        TagHierarchy tagHierarchy = new TagHierarchy();
        tagHierarchy.setName(tagHierarchyName);
        return tagHierarchyRepository.save(tagHierarchy);
    }

    @PostMapping(value = "/quickTagHierarchyCreate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public PersistentEntityResource quickTagHierarchyCreateFileCSV(
            @RequestParam("tagHierarchyName") String tagHierarchyName,
            @RequestParam("file") MultipartFile file,
            PersistentEntityResourceAssembler resourceAssembler) throws IOException {
        TagHierarchy tagHierarchy = createTagHierarchy(tagHierarchyName);
        readCSVAndSaveTags(file.getInputStream(), tagHierarchy);
        return resourceAssembler.toResource(tagHierarchy);
    }
}

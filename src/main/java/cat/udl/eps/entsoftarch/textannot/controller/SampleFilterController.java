package cat.udl.eps.entsoftarch.textannot.controller;

import cat.udl.eps.entsoftarch.textannot.domain.Sample;
import cat.udl.eps.entsoftarch.textannot.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.Serializable;

@BasePathAwareController
public class SampleFilterController {

    @Autowired
    private SampleRepository sampleRepository;

    @PostMapping("/samples/filter")
    public PagedResources<Sample> getFilteredSamples(@RequestBody SampleFilters filters, Pageable pageable, PagedResourcesAssembler resourceAssembler) {
        Page<Sample> samples = sampleRepository.findByTextContainingWord(filters.getWord(), pageable);
        return resourceAssembler.toResource(samples);
    }

    private static class SampleFilters implements Serializable {
        private String word;

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }
    }

}

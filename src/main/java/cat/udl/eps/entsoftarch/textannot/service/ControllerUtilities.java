package cat.udl.eps.entsoftarch.textannot.service;

import cat.udl.eps.entsoftarch.textannot.domain.QTag;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;
import cat.udl.eps.entsoftarch.textannot.exception.BadRequestException;
import cat.udl.eps.entsoftarch.textannot.repository.TagRepository;
import com.querydsl.core.BooleanBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ControllerUtilities {

    @Autowired
    TagRepository tagRepository;

    public List<Tag> getTagsFromIds(@RequestParam("tags") List<String> tags) {
        List<Tag> tagList = new ArrayList<>();
        for (String sTag : tags) {
            Optional<Tag> tag = tagRepository.findById(Integer.parseInt(sTag));
            if (!tag.isPresent())
                throw new BadRequestException();
            tagList.add(tag.get());
        }
        return tagList;
    }


    public BooleanBuilder getTagQuery(List<Tag> tags) {
        BooleanBuilder builder = new BooleanBuilder();
        for (Tag tag: tags) {
            builder.or(QTag.tag.treePath.contains(tag.getTreePath()));
        }
        return builder;
    }
}

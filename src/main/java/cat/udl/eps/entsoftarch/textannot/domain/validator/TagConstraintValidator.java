package cat.udl.eps.entsoftarch.textannot.domain.validator;
import cat.udl.eps.entsoftarch.textannot.domain.Tag;


import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class TagConstraintValidator implements ConstraintValidator <TagConstraint, Tag> {

    public void initialize(TagConstraint tag) {
    }

    @Override
    public boolean isValid(Tag tag, ConstraintValidatorContext context) {
        return tag.getParent() == null || tag.getProject() != null &&
                tag.getParent().getProject() != null &&
                tag.getProject().equals(tag.getParent().getProject());
    }
}

Feature: List all the annotations associated to a sample

  Scenario: List 5 Annotations referencing a Sample
    Given I login as "user" with password "password"
    Given I create a different sample with text "5RelatedAnnotations" with 5 related Annotations
    When  I search by Annotated as the last sample
    Then  I get a List with the said number of annotations
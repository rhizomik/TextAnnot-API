Feature: Create XML Sample
    In order to allow linguists to upload their XML samples
    As a linguist
    I want to create a new xml sample

  Background: Existing template
    Given there is a created Project with name "TemplateA"
    And The metadata template "TemplateA" has fields
      | datos_generales  | número_muestra      | STRING             | false |
      | datos_generales  | código_informante   | STRING             | false |
      | datos_generales  | transliteración     | STRING             | false |
      | datos_generales  | revisión_primera    | STRING             | false |
      | datos_generales  | revisión_segunda    | STRING             | false |
      | datos_generales  | etiquetado          | STRING             | false |
      | datos_muestra    | fecha_recogida      | STRING             | false |
      | datos_muestra    | palabras            | STRING             | false |
      | datos_muestra    | género_discursivo   | STRING             | false |
      | datos_muestra    | observaciones       | STRING             | false |
      | datos_informante | nombre              | STRING             | false |
      | datos_informante | sexo                | STRING             | false |
      | datos_informante | nivel_referencia    | STRING             | false |
      | datos_informante | curso               | STRING             | false |
      | datos_informante | universidad         | STRING             | false |
      | datos_informante | nivel_CET           | STRING             | false |
      | datos_informante | estancia_España     | STRING             | false |
      | datos_informante | observaciones       | STRING             | false |

  Scenario: Upload a XML file as linguist
    Given I login as "user" with password "password"
    When I upload the XML file with filename "0607_A1h1b0n20170523.xml" described by "TemplateA"
    Then The response code is 201
    And It has been created a XmlSample containing the text "¿Cómo es mi ciudad?"
    And It has been created a XmlSample with the following 7 values
      | datos_generales | número_muestra     | 0607                 |
      | datos_generales | código_informante  | 0607_A1h1b0n20170523 |
      | datos_generales | transliteración    | David Benioff        |
      | datos_muestra   | fecha_recogida     | 01-06-2017           |
      | datos_muestra   | palabras           | 13                   |
      | datos_muestra   | género_discursivo  | descripción          |
      | datos_muestra   | observaciones      | --                   |

  Scenario: Upload an original XML file as linguist
    Given I login as "user" with password "password"
    When I upload the XML file with filename "0608_A1m1b0n20170523.xml" described by "TemplateA"
    Then The response code is 201
    And It has been created a XmlSample containing the text "¿Cómo es mi ciudad?"
    And It has been created a XmlSample with the following 14 values
      | datos_generales  | número_muestra    | 0608                 |
      | datos_generales  | código_informante | 0608_A1m1b0n20170523 |
      | datos_generales  | transliteración   | David Benioff        |
      | datos_muestra    | fecha_recogida    | 23-05-2017           |
      | datos_muestra    | palabras          | 99                   |
      | datos_muestra    | género_discursivo | descripción          |
      | datos_informante | nombre            | Cersei Lannister     |
      | datos_informante | sexo              | mujer                |
      | datos_informante | nivel_referencia  | A1                   |
      | datos_informante | curso             | 2                    |
      | datos_informante | universidad       | King's University    |
      | datos_informante | nivel_CET         | no consta            |
      | datos_informante | estancia_España   | nunca                |
      | datos_informante | observaciones     | No ha anotado el curso, pero el texto está en el mismo paquete que todos los de la misma fecha. |

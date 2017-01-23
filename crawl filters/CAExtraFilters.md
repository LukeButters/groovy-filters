# CAExtraFilters filter

CAExtraFilters is a filter that extends the content auditor to provide a set of additional checks. 

The additional filters add content checking for:

Content existence (e.g. dc.author metadata must be set)
Content validation (e.g. title must ends with)
Content length (e.g. description must be shorter than 256 characters)

If CSS selector was defined in a rule on the first position in configuration files however it doesn't exist in webpage’s content, the CSS selector will be marked and displayed in Content Auditor as 'Missing tags'. For example 'ui.ca.check_element_length.cfg' contains rule 'h4,c,>,60'. If the <h4> heading tag doesn't exist in content so it will be added as not existing tag for this specific document.

## Usage

Copy the CAExtraFilters.groovy file to the collection's @groovy folder and add filter.CAExtraFilters to the filter.classes collection.cfg parameter.

Add individual rules to collection.cfg (see below) and display in the content auditor by adding the generated metadata fields to the various displays (controlled via collection.cfg options).

## Valid content checks
Rules defined in configuration file $SEARCH_HOME/conf/$COLLECTION_NAME/_default/ui.ca.check_element_content.cfg allows to check the content of HTML tags in a webpage. The file can be edited via Admin UI. One rule can be defined per line and need to follow below syntax:

```
CSS selector, [equal|has|startWith|endWith|notEqual|notHas|notStartWith|notEndWith],["string"|CSS selector]
```

**Generated metadata tags:**

The generated metadata tags follow the format:
```
FunElement[CSS selector][matchrule]
```

e.g. FunElementTitleNotEqual

These tags need to be mapped in metamap.cfg for use with content auditor.

**Examples:**

* ```title,endWith,"| My Site"```: <title> tag must end with " | My Site"
* ```title,notEqual,h1```: <title> tag  should match <h1> heading tag 
* ```title,notEqual,meta[name=DCTERMS.title]```: <title> tag should match metatag “DCTERMS.title”
* ```title,notEqual,#breadcrumbs li:last-child```: <title> tag should match the last item in breadcrumb 
* ```h1,notEqual,#nav-section .current-page```: Current page in navigation should match <h1> heading tag
* ```meta[name=description],notEqual,meta[name=DCTERMS.description]```: Metatag “description” should match metatag "DCTERMS.description" 
* ```meta[name=DCTERMS.identifier],notHas,www.example.com```: Metatag “DCTERMS.identifier” should not contain www.example.com

## Valid content length checks

Rules defined in configuration file $SEARCH_HOME/conf/$COLLECTION_NAME/_default/ui.ca.check_element_length.cfg allows to check the content length of HTML tags in a webpage. The file can be edited via Admin UI. One rule can be defined per line and need to follow below syntax:

'CSS selector,[c|w],[>|<|=],length' where c - characters, w – words

**Generated metadata tags:**

The generated metadata tags follow the format:
```
FunElement[CSS selector][C|W][Gt|Lt|Eq][length]
```

e.g. FunElementTitleGt100

These tags need to be mapped in metamap.cfg for use with content auditor.

**Examples**

```h1,c,>,60```: <h1> heading  should not be longer than 60 characters
```h2,c,>,60```: <h2> heading  should not be longer than 60 characters
```h3,c,>,60```: <h3> heading  should not be longer than 60 characters
```h4,c,>,60```: <h4> heading  should not be longer than 60 characters
```a,w,>,7```: Link text in <a> tag should not has more than 7 words

## Element existence checks

Rules defined in configuration file $SEARCH_HOME/conf/$COLLECTION_NAME/_default/ui.ca.exist_element.cfg allows to check if HTML element is specified in a webpage. The file can be edited via Admin UI. One rule can be defined per line and need to follow below syntax:

```
CSS selector
```

**Generated metadata tags:**

The generated metadata tags follow the format:
```
FunElementExist[CSS selector]
```

e.g. FunElementExistH1

**Examples**

```h1```: Checking the number of <h1> heading tags in content to detect if there is more than one
```h2```: Checking the number of <h2> heading tags in content to detect if there is more than one
```h1 a```: Checking if <h1> heading tag contains link
```meta[name=DCTERMS.license]```: Checking if document contains metatag “DCTERMS.license”

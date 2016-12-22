# Groovy filters

This repository contains reusable filters that can be added to a Funnelback collection to extend the filtering.

# Crawl filters

Crawl filters can be added to the main filter chain (filter.classes) and operate on whole documents as they are filtered during the gather phase.

See: [Developing custom filters](https://docs.funnelback.com/develop/programming-options/custom-filters.html)

## Included crawl filters



# Jsoup filters

Jsoup filters can be added to the Jsoup filter chain (filter.jsoup.classes).

Jsoup filters are used to transform HTML documents by operating on a Jsoup object representing the HTML structure.

See: [Jsoup filters](https://docs.funnelback.com/more/extra/filter_jsoup_classes_collection_cfg.html)

## Included Jsoup filters

* **Metadata delimiters:** Replace delimiters in specified metadata fields.
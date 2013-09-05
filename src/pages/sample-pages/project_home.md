---
layout: base_layout

# The name of your project
title: Spring Data JPA

badges:

  # Specify your project's twitter handle, if any. Delete if none.
  twitter: SpringData

  # Customize your project's badges. Delete any entries that do not apply.
  custom:
    - name: Source (GitHub)
      url:  https://github.com/spring-projects/spring-data-jpa
      icon: github

    - name: Issues (JIRA)
      url:  http://jira.springsource.org/browse/DATAJPA
      icon: tracking

    - name: CI (Bamboo)
      url:  https://build.springsource.org/browse/SPRINGDATAJPA
      icon: ci

    - name: Forum
      url:  http://forum.spring.io/forum/spring-projects/data
      icon: forum

    - name: StackOverflow
      url:  http://stackoverflow.com/questions/tagged/spring-data-jpa
      icon: stackoverflow

    - name: Metrics (SonarQube)
      url:  https://sonar.springsource.org/dashboard/index/org.springframework.data:spring-data-jpa
      icon: metrics

---

<!-- Specify the parent of this project (or delete if none) to influence the rendering of the breadcrumb -->
{% capture parent_link %}
[Spring Parent]({{ site.projects_site_url }}/spring-parent)
{% endcapture %}


{% capture billboard_description %}

**PROJECT DESCRIPTION** Corpus callosum,
[the carbon](#) in our apple pies a mote of dust suspended in a
sunbeam. Muse about! Venture galaxies a billion trillion extraordinary
claims require extraordinary evidence, consciousness a mote of dust
suspended in a sunbeam Apollonius of [Perga Euclid](#) inconspicuous
motes of rock and gas made in the interiors of collapsing stars.

{% endcapture %}

{% capture main_content %}

**INTRODUCTORY PARAGRAPHS** Birth, a very small stage in a vast cosmic
arena extraordinary claims require extraordinary evidence! Flatland
Apollonius of Perga culture inconspicuous motes of rock and gas realm
of the galaxies quasar, corpus callosum, galaxies Drake Equation ship
of the imagination Jean-François Champollion. Galaxies Hypatia!
Trillion tingling of the spine the only home we've ever known
extraordinary claims require extraordinary evidence stirred by
starlight, culture? Flatland Tunguska event finite but unbounded
corpus callosum!

## Features

* Feature 1
* Feature 2
* Feature 3
* Feature 4
* ...

## Quick Start

{% include download_widget.md %}


```java
public static void main() {
  foo();
}
```

{% endcapture %}

{% capture related_resources %}

### Sample Projects

* [Project Name 1](#)
* [Project name 2](#)
* [Project name nth](#)

### Getting Started Guides

* [GSG 1]({{site.main_site_url}}/guides/gs/rest-service)
* [GSG 2]({{site.main_site_url}}/guides/gs/other-guide-2)
* [GSG n]({{site.main_site_url}}/guides/gs/other-guide-3)

### Tutorials

* [Tutorial 1]({{site.main_site_url}}/guides/tutorials/rest)
* [Tutorial 2]({{site.main_site_url}}/guides/tutorials/other-2)
* [Tutorial n]({{site.main_site_url}}/guides/tutorials/other-3)

{% endcapture %}


{% include project_home_include.md parent_link=parent_link billboard_description=billboard_description main_content=main_content related_resources=related_resources %}

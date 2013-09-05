{% include documentation.md %}


{%unless badges contains 'not found in _includes directory' %}
<div class="right-pane-widget--container no-top-border">
<div class="project-sub-link--wrapper">
{% include badges.md %}
</div>
</div>
{%endunless%}

<div class="right-pane-widget--container no-top-border project-sidebar-resource--wrapper">
{{ include.related_resources | markdownify }}
</div>
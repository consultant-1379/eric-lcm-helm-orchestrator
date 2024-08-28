{{/*
Create a name by adding a list of suffixes to the chart name and check that the resulting name does
not exceed 253 characters.
*/}}
{{- define "eric-ran-rad-rce-base.create-name-253" -}}
    {{- $name := default .Chart.Name .Values.nameOverride -}}
    {{- $name := prepend .suffixes $name | compact | join "-" -}}
    {{- if gt (len $name) 253 -}}
        {{- printf "The name \"%s\" is longer than 253 characters" $name | fail -}}
    {{- end -}}
    {{- $name -}}
{{- end -}}


{{/*
Create a name by adding a list of suffixes to the chart name and check that the resulting name does
not exceed 63 characters.
*/}}
{{- define "eric-ran-rad-rce-base.create-name-63" -}}
    {{- $name := default .Chart.Name .Values.nameOverride -}}
    {{- $name := prepend .suffixes $name | compact | join "-" -}}
    {{- if gt (len $name) 63 -}}
        {{- printf "The name \"%s\" is longer than 63 characters" $name | fail -}}
    {{- end -}}
    {{- $name -}}
{{- end -}}


{{- define "eric-ran-rad-rce-base.label" -}}
{{ template "eric-ran-rad-rce-base.create-name-63" (merge (dict "suffixes" list) .) -}}
{{- end -}}

{{- define "eric-ran-rad-rce-base.pdb.name" -}}
{{- template "eric-ran-rad-rce-base.create-name-253" (merge (dict "suffixes" (list "pdb")) .) -}}
{{- end -}}


{{- define "eric-ran-rad-rce-base.security-policy.name" -}}
{{- template "eric-ran-rad-rce-base.create-name-253" (merge (dict "suffixes" (list "security-policy")) .) -}}
{{- end -}}


{{- define "eric-ran-rad-rce-base.service-account.name" -}}
{{- template "eric-ran-rad-rce-base.create-name-253" (merge (dict "suffixes" (list "service-account")) .) -}}
{{- end -}}


{{/*
Pod name for the Helm test pod.
*/}}
{{- define "eric-ran-rad-rce-base.helm-test-pod-name" -}}
    {{- template "eric-ran-rad-rce-base.create-name-253" (merge (dict "suffixes" (list "test")) .) -}}
{{- end -}}
